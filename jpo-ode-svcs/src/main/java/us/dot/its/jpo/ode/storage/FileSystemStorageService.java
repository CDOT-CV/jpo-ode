/*============================================================================
 * Copyright 2018 572682
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package us.dot.its.jpo.ode.storage;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;
import us.dot.its.jpo.ode.coder.stream.FileImporterProperties;

/**
 * A service class responsible for file storage and management operations in the filesystem.
 * It provides methods to store, load, retrieve, and delete files. This service is implemented
 * to handle specific log file types and maintain files in organized directories.
 */
@Service
@Slf4j
public class FileSystemStorageService implements StorageService {

  private final Path rootLocation;
  private final Path logFileLocation;

  /**
   * Constructs a FileSystemStorageService instance and initializes the file storage locations
   * based on the provided properties.
   *
   * @param properties The configuration properties used to determine the root and OBU log file upload locations.
   */
  @Autowired
  public FileSystemStorageService(FileImporterProperties properties) {

    this.rootLocation = Paths.get(properties.getUploadLocationRoot());
    this.logFileLocation = Paths.get(properties.getUploadLocationRoot(),
        properties.getObuLogUploadLocation());

    log.info("Upload location (root): {}", this.rootLocation);
    log.info("Upload location (OBU log file): {}", this.logFileLocation);
  }

  /**
   * Stores a given MultipartFile in the appropriate directory based on the provided file type.
   *
   * <p>The method determines the destination path for the file based on its {@link LogFileType}.
   * It first verifies that the file is not empty and deletes any existing file with the same name
   * in the target directory. Once these checks are completed, it copies the file to the resolved path.
   * If any of these operations fail, a {@link StorageException} is thrown.
   *
   * @param file The file to be stored. Must not be empty.
   * @param type The type of the file, which determines the destination path. Expected types include
   *             {@code LogFileType.BSM}, {@code LogFileType.OBU}, or {@code LogFileType.UNKNOWN}.
   *
   * @throws StorageException If the file type is unknown, the file is empty, an error occurs
   *                          while deleting an existing file, or an error occurs during file storage.
   */
  @Override
  public void store(MultipartFile file, LogFileType type) {

    // Discern the destination path via the file type (bsm or messageFrame)
    Path path;
    switch (type) {
      case BSM, OBU -> path = this.logFileLocation.resolve(Objects.requireNonNull(file.getOriginalFilename()));
      case UNKNOWN -> {
        log.error("File type unknown: {} {}", type, file.getName());
        throw new StorageException("File type unknown: " + type + " " + file.getName());
      }
      default -> throw new StorageException("File type unknown: " + type + " " + file.getName());
    }

    // Check file is not empty
    if (file.isEmpty()) {
      log.error("File is empty: {}", path);
      throw new StorageException("File is empty: " + path);
    }

    // Check file does not already exist (if so, delete existing)
    try {
      log.info("Deleting existing file: {}", path);
      Files.deleteIfExists(path);
    } catch (IOException e) {
      log.error("Failed to delete existing file: {} ", path);
      throw new StorageException("Failed to delete existing file: " + path, e);
    }

    // Copy the file to the relevant directory
    try {
      log.debug("Copying file {} to {}", file.getOriginalFilename(), path);
      log.info("Copying file {} to {}", file.getOriginalFilename(), path);
      Files.copy(file.getInputStream(), path);
    } catch (Exception e) {
      log.error("Failed to store file in shared directory {}", path);
      throw new StorageException("Failed to store file in shared directory " + path, e);
    }
  }

  @Override
  public Stream<Path> loadAll() {
    try {
      return Files.walk(this.rootLocation, 1).filter(path -> !path.equals(this.rootLocation))
          .map(path -> this.rootLocation.relativize(path));
    } catch (IOException e) {
      log.error("Failed to read files stored in {}", this.rootLocation);
      throw new StorageException("Failed to read files stored in " + this.rootLocation, e);
    }
  }

  @Override
  public Path load(String filename) {
    return rootLocation.resolve(filename);
  }

  @Override
  public Resource loadAsResource(String filename) {
    try {
      Path file = load(filename);
      Resource resource = new UrlResource(file.toUri());
      if (resource.exists() && resource.isReadable()) {
        return resource;
      } else {
        throw new StorageFileNotFoundException("Could not read file: " + filename);
      }
    } catch (MalformedURLException e) {
      throw new StorageFileNotFoundException("Could not read file: " + filename, e);
    }
  }

  @Override
  public void deleteAll() {
    log.info("Deleting all files and directories in {}", this.rootLocation);
    FileSystemUtils.deleteRecursively(rootLocation.toFile());
  }

  @Override
  public void init() {
    try {
      Files.createDirectory(rootLocation);
    } catch (IOException e) {
      log.error("Failed to initialize storage service {}", this.rootLocation);
      throw new StorageException("Failed to initialize storage service " + this.rootLocation, e);
    }
  }
}
