# JPO Quarterly Release Process
The quarterly release process is designed to prepare the code for a new release at the end of each quarter. This process involves several steps: creating a new release branch, stabilizing the code, updating project references, building the release, and testing it. These steps are performed sequentially for each project, starting with those that have no dependencies and progressing outward through the dependency chain.

The order of project releases is as follows:
1. Stage 1:
   1. [asn1_codec](#asn1_codec)
   2. [jpo-cvdp](#jpo-cvdp)
   3. [jpo-s3-deposit](#jpo-s3-deposit)
   4. [jpo-security-svcs](#jpo-security-svcs)
   5. [jpo-sdw-depositor](#jpo-sdw-depositor)
   6. [jpo-utils](#jpo-utils)
   7. [jpo-asn-pojos](#jpo-asn-pojos)
2. Stage 2:
   1. [jpo-ode](#jpo-ode)
   2. [j2735-ffm-java](#j2735-ffm-java)
3. Stage 3:
   1. [jpo-geojsonconverter](#jpo-geojsonconverter)
   2. jpo-mec-deposit
4. Stage 4:
   1. [jpo-conflictmonitor](#jpo-conflictmonitor)
   2. [jpo-deduplicator](#jpo-deduplicator)
5. Stage 5:
   1. [jpo-cvmanager](#jpo-cvmanager)


## asn1_codec
### Prerequisites
None

### 1. Code Ready & Release Notes
    - [ ] Release notes drafted & added to `Release_notes.md` file in `docs` directory
    - [ ] Code changes for release are merged into `develop`
    - [ ] A new branch `release/(year)-(quarter)` is created from `develop`

### 2. Preliminary Testing
    - [ ] code compiles
    - [ ] unit tests pass
    - [ ] program starts up correctly
    - [ ] program can be configured for decoding successfully
    - [ ] program can be configured for encoding successfully
    - [ ] messages get decoded as expected
    - [ ] messages get encoded as expected

### 3. Project Reference Updates & Release Creation
    - [ ] Merge ‘release/(year)-(quarter)’ branch into ‘master’ branch for the asn1_codec project
    - [ ] Create a release for the asn1_codec project from the ‘master’ branch and tag the release with the version number of the release. (e.g. asn1_codec-x.x.x)
    - [ ] Create docker image
        - [ ] Use the release for the asn1_codec to produce docker image with the same tag name, containing the version number.
        - [ ] Upload docker image to [DockerHub](https://hub.docker.com/u/usdotjpoode)
        - [ ] Tag docker image with the version number of the app. (e.g. 1.0.0)
        - [ ] Tag docker image with year and quarter of release. (e.g. 2024-Q2)
        - [ ] Tag docker image with 'latest' tag for the most recent release.
    - [ ] Merge master branch into develop branch & verify that CI/CD passes.

### 4. DockerHub Image Testing
    - [ ] image starts up correctly
    - [ ] program can be configured for decoding successfully
    - [ ] program can be configured for encoding successfully
    - [ ] messages get decoded as expected
    - [ ] messages get encoded as expected


## jpo-cvdp
### Prerequisites
None

### 1. Code Ready & Release Notes
    - [ ] Release notes drafted & added to `Release_notes.md` file in `docs` directory
    - [ ] Code changes for release are merged into `develop`
    - [ ] A new branch `release/(year)-(quarter)` is created from `develop`

### 2. Preliminary Testing
    - [ ] code compiles
    - [ ] unit tests pass
    - [ ] program starts up correctly
    - [ ] messages get consumed as expected
    - [ ] BSMs inside geofence are retained
    - [ ] BSMs with a partII section are retained
    - [ ] BSMs outside geofence are suppressed
    - [ ] BSMs above speed range are suppressed
    - [ ] BSMs below speed range are suppressed

### 3. Project Reference Updates & Release Creation
    - [ ] Merge ‘release/(year)-(quarter)’ branch into ‘master’ branch for the jpo-cvdp project
    - [ ] Create a release for the jpo-cvdp project from the ‘master’ branch and tag the release with the version number of the release. (e.g. jpo-cvdp-x.x.x)
    - [ ] Create docker image
        - [ ] Use the release for the jpo-cvdp to produce docker image with the same tag name, containing the version number.
        - [ ] Upload docker image to [DockerHub](https://hub.docker.com/u/usdotjpoode)
        - [ ] Tag docker image with the version number of the app. (e.g. 1.0.0)
        - [ ] Tag docker image with year and quarter of release. (e.g. 2024-Q2)
        - [ ] Tag docker image with 'latest' tag for the most recent release.
    - [ ] Merge master branch into develop branch & verify that CI/CD passes.

### 4. DockerHub Image Testing
    - [ ] image starts up correctly
    - [ ] messages get consumed as expected
    - [ ] BSMs inside geofence are retained
    - [ ] BSMs with a partII section are retained
    - [ ] BSMs outside geofence are suppressed
    - [ ] BSMs above speed range are suppressed
    - [ ] BSMs below speed range are suppressed


## jpo-security-svcs
### Prerequisites
None

### 1. Code Ready & Release Notes
    - [ ] Release notes drafted & added to `Release_notes.md` file in `docs` directory
    - [ ] Code changes for release are merged into `develop`
    - [ ] A new branch `release/(year)-(quarter)` is created from `develop`

### 2. Preliminary Testing
    - [ ] code compiles
    - [ ] program starts up correctly
    - [ ] program can be successfully configured
    - [ ] messages can be successfully signed

### 3. Project Reference Updates & Release Creation
    - [ ] Update version number in pom.xml if not already done
    - [ ] Merge ‘release/(year)-(quarter)’ branch into ‘master’ branch for the jpo-security-svcs project
    - [ ] Create a release for the jpo-security-svcs project from the ‘master’ branch and tag the release with the version number of the release. (e.g. jpo-security-svcs-x.x.x)
    - [ ] Create docker image
        - [ ] Use the release for the jpo-security-svcs to produce docker image with the same tag name, containing the version number.
        - [ ] Upload docker image to [DockerHub](https://hub.docker.com/u/usdotjpoode)
        - [ ] Tag docker image with the version number of the app. (e.g. 1.0.0)
        - [ ] Tag docker image with year and quarter of release. (e.g. 2024-Q2)
        - [ ] Tag docker image with 'latest' tag for the most recent release.
    - [ ] Merge master branch into develop branch & verify that CI/CD passes.

### 4. DockerHub Image Testing
    - [ ] image starts up correctly
    - [ ] program can be successfully configured
    - [ ] messages can be successfully signed


## jpo-sdw-depositor
### Prerequisites
None

### 1. Code Ready & Release Notes
    - [ ] Release notes drafted & added to `Release_notes.md` file in `docs` directory
    - [ ] Code changes for release are merged into `develop`
    - [ ] A new branch `release/(year)-(quarter)` is created from `develop`

### 2. Preliminary Testing
    - [ ] code compiles
    - [ ] unit tests pass
    - [ ] program starts up correctly
    - [ ] messages are consumed successfully
    - [ ] messages are submitted to the SDX successfully

### 3. Project Reference Updates & Release Creation
    - [ ] Update version number in pom.xml if not already done
    - [ ] Merge ‘release/(year)-(quarter)’ branch into ‘master’ branch for the jpo-sdw-depositor project
    - [ ] Create a release for the jpo-sdw-depositor project from the ‘master’ branch and tag the release with the version number of the release. (e.g. jpo-sdw-depositor-x.x.x)
    - [ ] Create docker image
        - [ ] Use the release for the jpo-sdw-depositor to produce docker image with the same tag name, containing the version number.
        - [ ] Upload docker image to [DockerHub](https://hub.docker.com/u/usdotjpoode)
        - [ ] Tag docker image with the version number of the app. (e.g. 1.0.0)
        - [ ] Tag docker image with year and quarter of release. (e.g. 2024-Q2)
        - [ ] Tag docker image with 'latest' tag for the most recent release.
    - [ ] Merge master branch into develop branch & verify that CI/CD passes.

### 4. DockerHub Image Testing
    - [ ] image starts up correctly
    - [ ] messages are consumed successfully
    - [ ] messages are submitted to the SDX successfully


## jpo-utils
### Prerequisites
None

### 1. Code Ready & Release Notes
    - [ ] Release notes drafted & added to `Release_notes.md` file in `docs` directory
    - [ ] Code changes for release are merged into `develop`
    - [ ] A new branch `release/(year)-(quarter)` is created from `develop`

### 2. Preliminary Testing
    - [ ] jpo-jikkou docker image builds
    - [ ] jpo-kafka-connect image builds
        - [ ] Verify all connectors are created using connect rest api

### 3. Project Reference Updates & Release Creation
    - [ ] Update version number in pom.xml files for the 'jpo-utils' project if not already done.
    - [ ] Merge ‘release/(year)-(quarter)’ branch into ‘master’ branch for the jpo-sdw-depositor project
    - [ ] Create a release for the jpo-utils project from the ‘master’ branch and tag the release with the version number of the release. (e.g. jpo-utils-x.x.x)
    - [ ] Create docker images
        - [ ] Use the release for the jpo-utils to produce docker images 'jpo-jikkou' & 'jpo-kafka-connect', containing the version number.
        - [ ] Upload docker images to [DockerHub](https://hub.docker.com/u/usdotjpoode)
        - [ ] Tag docker images with the version number of the app. (e.g. 1.0.0)
        - [ ] Tag docker images with year and quarter of release. (e.g. 2024-Q2)
        - [ ] Tag docker images with 'latest' tag for the most recent release.
    - [ ] Merge master branch into develop branch & verify that CI/CD passes.

### 4. DockerHub Image Testing
    - [ ] Change the image references in `docker-compose-kafka.yml` and `docker-compose-connect.yml` to the version number of the release. (e.g. 1.0.0)
        - [ ] `docker-compose-kafka.yml` services changed:
            - [ ] kafka-setup image: `usdotjpoode/jpo-jikkou:1.0.0`
        - [ ] `docker-compose-connect.yml` services changed:
            - [ ] kafka-connect image: `usdotjpoode/jpo-kafka-connect:1.0.0`
            - [ ] kafka-connect-setup image: `usdotjpoode/jpo-jikkou:1.0.0`
    - [ ] jpo-jikkou docker image runs successfully
    - [ ] jpo-kafka-connect image runs successfully
        - [ ] Verify all connectors are created using connect rest api

## jpo-asn-pojos
### Prerequisites
None

### 1. Code Ready & Release Notes
    - [ ] Release notes drafted & added to `Release_notes.md` file in `docs` directory in this repo
    - [ ] Code changes for release are merged into `develop` in the `jpo-asn-pojos` repo
    - [ ] A new branch `release/(year)-(quarter)` is created from `develop` in the `jpo-asn-pojos` repo

### 2. Preliminary Testing
    - [ ] code compiles
        - gradle build
            - [ ] `jpo-asn-j2735-2024` subproject builds via gradle
            - [ ] `jpo-asn-test-generator` subproject build via gradle
        - maven build:
            - [ ] `jpo-asn-j2735-2024` subproject builds via maven
    - [ ] unit tests pass
    - [ ] test generator command line tool can generate messages

### 3. Project Reference Updates & Release Creation
    - [ ] Update Gradle build version number
        - [ ] Update version number in 'jpo-asn-pojos/jpo-asn-runtime/gradle.build'
        - [ ] Update version number in 'jpo-asn-pojos/jpo-asn-j2735-2024/gradle.build'
    - [ ] Udate Maven build version number
        - [ ] Update version number in 'jpo-asn-pojos/pom.xml'
        - [ ] Update version number in 'jpo-asn-pojos/jpo-asn-runtime/pom.xml'
        - [ ] Update version number in `jpo-asn-pojos/jpo-asn-j2735-2024/pom.xml`
    - [ ] Merge the 'release/(year)-(quarter)' branch into the 'master' branch of the 'jpo-asn-pojos' project, and create a release and git tag with the version number of the release. (i.e. jpo-asn-pojos-x.x.x)
    - [ ] Merge 'master' branch into 'develop' branch & verify that CI/CD passes.

### 4. DockerHub Image Testing
Not applicable

## jpo-ode
### Prerequisites
    - [ ] asn1_codec release completed
    - [ ] jpo-cvdp release completed
    - [ ] jpo-security-svcs release completed
    - [ ] jpo-sdw-depositor release completed
    - [ ] jpo-utils release completed
    - [ ] jpo-asn-pojos release completed

#### Dependency Types
| Dependency | Type |
| --- | --- |
| asn1_codec | Git Submodule |
| jpo-cvdp | Git Submodule |
| jpo-security-svcs | Git Submodule |
| jpo-sdw-depositor | Git Submodule |
| jpo-utils | Git Submodule |
| jp-asn-pojos | Git Submodule |

### 1. Code Ready & Release Notes
    - [ ] Release notes drafted & added to `Release_notes.md` file in `docs` directory
    - [ ] Code changes for release are merged into `develop`
    - [ ] A new branch `release/(year)-(quarter)` is created from `develop`

### 2. Preliminary Testing
    - [ ] code compiles
    - [ ] unit tests pass
    - [ ] program starts up correctly
    - [ ] http endpoint (web page) is reachable
    - [ ] capable of ingesting messages via udp (see scripts in `scripts/tests` directory)
        - [ ] TIMs
        - [ ] BSMs
        - [ ] SSMs
        - [ ] SRMs
        - [ ] SPaTs
        - [ ] Maps
        - [ ] PSMs
        - [ ] SDSMs
        - [ ] RTCMs
        - [ ] RSMs
    - [ ] TIMs can make it through the entire pipeline successfully (integration)
        - [ ] TIMs can be pushed to http (/tim) endpoint
        - [ ] TIMs can be encoded & re-ingested into the ODE
        - [ ] TIMs can be signed (verify security header is not stripped)
        - [ ] TIMs can be sent to the SDX
    - [ ] multiple ODE instances function correctly when running against shared Kafka
        - [ ] no K-Table conflicts occur between instances
        - [ ] message ingestion works reliably across instances for all message types
            - [ ] TIMs
            - [ ] BSMs
            - [ ] SSMs
            - [ ] SRMs
            - [ ] SPaTs
            - [ ] Maps
            - [ ] PSMs
            - [ ] SDSMs
            - [ ] RTCMs
            - [ ] RSMs
        - [ ] message processing pipeline functions correctly across instances
        - [ ] encoding/decoding works properly with multiple instances
        - [ ] message signing functions correctly with multiple instances
        - [ ] SDX deposit works correctly with multiple instances

### 3. Project Reference Updates & Release Creation
    - [ ] Update version number in pom.xml files for the 'jpo-ode' project if not already done.
        - [ ] Open the jpo-ode project in an IDE
        - [ ] Update the version number in the pom.xml files of the jpo-ode-common, jpo-ode-plugins, and jpo-ode-svcs projects to match the version number of the release. (e.g. 1.0.0)
        - [ ] Update the dependencies in pom.xml files of the jpo-ode-common, jpo-ode-plugins, and jpo-ode-svcs projects to the version number set in the previous step. The `jpo-ode-plugins` depends on the `jpo-ode-common` project, and the `jpo-ode-svcs` project depends on the `jpo-ode-core` and `jpo-ode-plugins` projects. These should all be referencing the same version number. (e.g. 1.0.0)
    - [ ] Update git submodule references for the ‘jpo-ode’ project to point to tagged commits in projects with updated `master` branches.
        - [ ] Open the jpo-ode project in an IDE.
        - [ ] Navigate to the asn1_codec directory and run `git checkout tags/asn1_codec-x.x.x` to update the submodule reference.
        - [ ] Navigate to the jpo-cvdp directory and run `git checkout tags/jpo-cvdp-x.x.x` to update the submodule reference.
        - [ ] Navigate to the jpo-security-svcs directory and run `git checkout tags/jpo-security-svcs-x.x.x` to update the submodule reference.
        - [ ] Navigate to the jpo-sdw-depositor directory and run `git checkout tags/jpo-sdw-depositor-x.x.x` to update the submodule reference.
        - [ ] Navigate to the jpo-utils directory and run `git checkout tags/jpo-utils-x.x.x` to update the submodule reference. 
        - [ ] Navigate to the jpo-asn-pojos directory and run 'git checkout 'jpo-asn-pojos-x.x.x' to update the submodule reference.
        - [ ] Commit these changes to the `release/(year)-(quarter)` branch & push the changes to the remote repository.
        - [ ] Ensure these changes pass CI/CD checks before continuing.
    - [ ] Merge `release/(year)-(quarter)` branch into `master` branch for the jpo-ode project, and create a release with the version number of the release. (e.g. jpo-ode-x.x.x)
    - [ ] Create docker image
        - [ ] Use the release for the jpo-ode to produce docker image with the same tag name, containing the version number.
        - [ ] Upload docker image to [DockerHub](https://hub.docker.com/u/usdotjpoode)
        - [ ] Tag docker image with the version number of the app. (e.g. 1.0.0)
        - [ ] Tag docker image with year and quarter of release. (e.g. 2024-Q2)
        - [ ] Tag docker image with 'latest' tag for the most recent release.
    - [ ] Merge master branch into develop branch & verify that CI/CD passes.

### 4. DockerHub Image Testing
    - [ ] image starts up correctly
    - [ ] http endpoint (web page) is reachable
    - [ ] capable of ingesting messages via udp (see scripts in `scripts/tests` directory)
        - [ ] TIMs
        - [ ] BSMs
        - [ ] SSMs
        - [ ] SRMs
        - [ ] SPaTs
        - [ ] Maps
        - [ ] PSMs
        - [ ] SDSMs
        - [ ] RTCMs
        - [ ] RSMs
    - [ ] TIMs can make it through the entire pipeline successfully (integration)
        - [ ] TIMs can be pushed to http (/tim) endpoint
        - [ ] TIMs can be encoded & re-ingested into the ODE
        - [ ] TIMs can be signed (verify security header is not stripped)
        - [ ] TIMs can be sent to the SDX
    - [ ] multiple ODE instances function correctly when running against shared Kafka
        - [ ] no K-Table conflicts occur between instances
        - [ ] message ingestion works reliably across instances for all message types
            - [ ] TIMs
            - [ ] BSMs
            - [ ] SSMs
            - [ ] SRMs
            - [ ] SPaTs
            - [ ] Maps
            - [ ] PSMs
            - [ ] SDSMs
            - [ ] RTCMs
            - [ ] RSMs
        - [ ] message processing pipeline functions correctly across instances
        - [ ] encoding/decoding works properly with multiple instances
        - [ ] message signing functions correctly with multiple instances
        - [ ] SDX deposit works correctly with multiple instances

## j2735-ffm-java

### Prerequisites

- [ ] asn1_codec release completed

#### Dependency Types

| Dependency | Type          |
| --- |---------------|
| neaeraconsulting/j2735-ffm-java | Upstream |
| asn1_codec | Git Submodule |

### 1. Code Ready & Release Notes

- [ ] The upstream release branch exists, e.g. 'release/2025-q3'
- [ ] Release notes have been drafted & added to `RELEASE-NOTES.md` file in the upstream release branch
- [ ] Use the GitHub 'new branch' GUI to create a new branch with the same name as the upstream release branch, with source being the upstream release branch.
- [ ] Do "sync fork" on the upstream release branch to pull in upstream updates.

### 2. Preliminary Testing

- [ ] code compiles
   - [ ] `j2735-2024-ffm-lib` subproject builds via gradle
   - [ ] `j2735-2024-api` subproject builds via gradle
- [ ] unit tests in the `j2735-2024-ffm-lib` project pass
- [ ] integration tests using the http api pass via:
  - `docker compose -f docker-compose-api.yml up --build -d`
  - Run .http tests in `j2735-2024-api/http-tests`
- [ ] rebuilding the native library and FFM generated code succeeds via:
  - `docker compose -f docker-compose-build.yml up --build -d`

### 3. Project Reference Updates & Release Creation
- [ ] Make sure the Gradle version number for the Java library has been updated in `j2735-2024-ffm-lib/build.gradle`
- [ ] Create git tag for the release with the version number of the release. (i.e. vX.Y.Z), using the same version number of the Java library from `build.gradle`
- [ ] Create a release from the git tag.  
  - [ ] Copy the release notes into the markdown of the release.  
  - [ ] Include links to the native libraries in the release notes.
- [ ] After creating the release, verify the Jar library was published to GitHub Maven.

### 4. DockerHub Image Testing
Not applicable



## jpo-geojsonconverter
### Prerequisites
    - [ ] jpo-ode release completed
    - [ ] jpo-utils release completed

#### Dependency Types
| Dependency | Type |
| --- | --- |
| jpo-ode | Maven, DockerHub |
| jpo-utils | Git Submodule |

### 1. Code Ready & Release Notes
    - [ ] Release notes drafted & added to `Release_notes.md` file in `docs` directory
    - [ ] Code changes for release are merged into `develop`
    - [ ] A new branch `release/(year)-(quarter)` is created from `develop`

### 2. Preliminary Testing
    - [ ] code compiles
    - [ ] unit tests pass
    - [ ] program starts up correctly
    - [ ] program can be configured successfully
    - [ ] The following ODE messages are consumed in the GJC:
      - [ ] BSMs
      - [ ] PSMs
      - [ ] MAPs
      - [ ] SPaTs
      - [ ] SSMs
      - [ ] SRMs
      - [ ] RTCMs
    - [ ] Valid processed messages are outputted in Kafka on the following topics:  
      - [ ] Processed BSM - `topic.ProcessedBsm`  
      - [ ] Processed PSM - `topic.ProcessedPsm`  
      - [ ] Processed MAP - `topic.ProcessedMap`  
      - [ ] Processed SPaT - `topic.ProcessedSpat`  
      - [ ] Processed SSM - `topic.ProcessedSsm`  
      - [ ] Processed SRM - `topic.ProcessedSrm`  
      - [ ] Processed RTCM - `topic.ProcessedRtcm`  

### 3. Project Reference Updates & Release Creation
    - [ ] Update version number in pom.xml file for the `jpo-geojsonconverter` project if not already done.
    - [ ] Update git submodule & artifact references for the ‘jpo-geojsonconverter’ project.
        - [ ] Open the jpo-geojsonconverter project in an IDE.
        - [ ] Navigate to the jpo-utils directory and run `git checkout tags/jpo-utils-x.x.x` to update the submodule reference.
        - [ ] Update the version number in the pom.xml for the jpo-ode-core and jpo-ode-plugins dependencies to match the version number of the corresponding releases. (e.g. 1.0.0)
        - [ ] Commit these changes to the `release/(year)-(quarter)` branch & push the changes to the remote repository.
        - [ ] Ensure these changes pass CI/CD checks before continuing.
    - [ ] Merge `release/(year)-(quarter)` branch into `master` branch for the jpo-geojsonconverter project, and create a release with the version number of the release. (e.g. jpo-geojsonconverter-x.x.x)
    - [ ] Create docker image
        - [ ] Use the release for the jpo-geojsonconverter to produce docker image with the same tag name, containing the version number.
        - [ ] Upload docker image to [DockerHub](https://hub.docker.com/u/usdotjpoode)
        - [ ] Tag docker image with the version number of the app. (e.g. 1.0.0)
        - [ ] Tag docker image with year and quarter of release. (e.g. 2024-Q2)
        - [ ] Tag docker image with 'latest' tag for the most recent release.
    - [ ] Merge master branch into develop branch & verify that CI/CD passes.

### 4. DockerHub Image Testing
    - [ ] image starts up correctly
    - [ ] program can be configured successfully
    - [ ] The following ODE messages are consumed in the GJC:
      - [ ] BSMs
      - [ ] PSMs
      - [ ] MAPs
      - [ ] SPaTs
      - [ ] SSMs
      - [ ] SRMs
      - [ ] RTCMs
    - [ ] Valid processed messages are outputted in Kafka on the following topics:  
      - [ ] Processed BSM - `topic.ProcessedBsm`  
      - [ ] Processed PSM - `topic.ProcessedPsm`  
      - [ ] Processed MAP - `topic.ProcessedMap`  
      - [ ] Processed SPaT - `topic.ProcessedSpat`  
      - [ ] Processed SSM - `topic.ProcessedSsm`  
      - [ ] Processed SRM - `topic.ProcessedSrm`  
      - [ ] Processed RTCM - `topic.ProcessedRtcm`  


## jpo-conflictmonitor
### Prerequisites
    - [ ] jpo-ode release completed
    - [ ] jpo-geojsonconverter release completed
    - [ ] jpo-utils release completed

#### Dependency Types
| Dependency | Type |
| --- | --- |
| jpo-ode | Maven, DockerHub |
| jpo-geojsonconverter | Maven, DockerHub |
| jpo-utils | Git Submodule |

### 1. Code Ready & Release Notes
    - [ ] Release notes drafted & added to `Release_notes.md` file in `docs` directory
    - [ ] Code changes for release are merged into `develop`
    - [ ] A new branch `release/(year)-(quarter)` is created from `develop`

### 2. Preliminary Testing
    - [ ] code compiles
    - [ ] unit tests pass
    - [ ] program starts up correctly
    - [ ] program processes SpAT/Map/BSM messages and generates events as expected (see https://github.com/usdot-jpo-ode/jpo-conflictmonitor/wiki/Integration-Tests)
        - [ ] test BSM events
        - [ ] test connection of travel event
        - [ ] test intersection reference alignment events
        - [ ] test lane direction of travel event
        - [ ] test Map broadcast rate event
        - [ ] test Map minimum data event
        - [ ] test signal group alignment events
        - [ ] test signal state conflict events
        - [ ] test SPaT broadcast rate event
        - [ ] test SPaT minimum data event
        - [ ] test SPaT time change details event
        - [ ] test stop line passage events
        - [ ] test stop line stop events

### 3. Project Reference Updates & Release Creation
    - [ ] Update version number in pom.xml file for the `jpo-conflictmonitor` project if not already done.
    - [ ] Update git submodule & artifact references for the ‘jpo-conflictmonitor project.
        - [ ] Open the jpo-conflictmonitor project in an IDE.
        - [ ] Navigate to the jpo-utils directory and run `git checkout tags/jpo-utils-x.x.x` to update the submodule reference.
        - [ ] Update the version number in the pom.xml files for the jpo-geojsonconverter and jpo-ode-* dependencies to match the version number of the corresponding releases. (e.g. 1.0.0)
            - [ ] Update the jpo-conflictmonitor/pom.xml
            - [ ] Update the message-sender/pom.xml
        - [ ] Commit these changes to the `release/(year)-(quarter)` branch & push the changes to the remote repository.
        - [ ] Ensure these changes pass CI/CD checks before continuing.
    - [ ] Merge `release/(year)-(quarter)` branch into `master` branch for the jpo-conflictmonitor project, and create a release with the version number of the release. (e.g. jpo-conflictmonitor-x.x.x)
    - [ ] Create docker image
        - [ ] Use the release for the jpo-conflictmonitor to produce docker image with the same tag name, containing the version number.
        - [ ] Upload docker image to [DockerHub](https://hub.docker.com/u/usdotjpoode)
        - [ ] Tag docker image with the version number of the app. (e.g. 1.0.0)
        - [ ] Tag docker image with year and quarter of release. (e.g. 2024-Q2)
        - [ ] Tag docker image with 'latest' tag for the most recent release.
    - [ ] Merge master branch into develop branch & verify that CI/CD passes.

### 4. DockerHub Image Testing
    - [ ] image starts up correctly
    - [ ] program processes SpAT/Map/BSM messages and generates events as expected (see https://github.com/usdot-jpo-ode/jpo-conflictmonitor/wiki/Integration-Tests)
        - [ ] test BSM events
        - [ ] test connection of travel event
        - [ ] test intersection reference alignment events
        - [ ] test lane direction of travel event
        - [ ] test Map broadcast rate event
        - [ ] test Map minimum data event
        - [ ] test signal group alignment events
        - [ ] test signal state conflict events
        - [ ] test SPaT broadcast rate event
        - [ ] test SPaT minimum data event
        - [ ] test SPaT time change details event
        - [ ] test stop line passage events
        - [ ] test stop line stop events


## jpo-deduplicator
### Prerequisites
    - [ ] jpo-ode release completed
    - [ ] jpo-geojsonconverter release completed
    - [ ] jpo-utils release completed

#### Dependency Types
| Dependency | Type |
| --- | --- |
| jpo-ode | Maven |
| jpo-geojsonconverter | Maven |
| jpo-utils | Git Submodule |

### 1. Code Ready & Release Notes
    - [ ] Release notes drafted & added to `Release_notes.md` file in `docs` directory
    - [ ] Code changes for release are merged into `develop`
    - [ ] A new branch `release/(year)-(quarter)` is created from `develop`

### 2. Preliminary Testing
    - [ ] deduplicator code compiles
    - [ ] deduplicator unit tests pass
    - [ ] program starts up correctly
    - [ ] program processes BSM, SPaT, Map, and TIM messages
    - [ ] ProcessedMap, ProcessedSpat, OdeMap, OdeTim, OdeRawEncodedTim, OdeBsm messages are consumed
    - [ ] A single message is output for each of the input types

### 3. Project Reference Updates & Release Creation
    - [ ] Update version number in pom.xml file for the `jpo-deduplicator` project if not already done.
    - [ ] Update the corresponding version number for the jpo-geojsonconverter and jpo-ode-* dependencies in the pom.xml files of the jpo-deduplicator project. This change will be necessary in the jpo-deduplicator/pom.xml file.
    - [ ] Update git submodule references for the ‘jpo-ode’ project to point to tagged commits in projects with updated `master` branches.
        - [ ] Open the jpo-ode project in an IDE.
        - [ ] Navigate to the jpo-utils directory and run `git checkout tags/jpo-utils-x.x.x` to update the submodule reference.
    - [ ] Merge `release/(year)-(quarter)` branch into `master` branch for the jpo-deduplicator project, update the existing release tag (e.g. jpo-deduplicator-x.x.x) to point to the newly committed version of jpo-deduplicator
    - [ ] Create docker image
        - [ ] Use the release for the jpo-deduplicator to produce docker image with the same tag name, containing the version number.
        - [ ] Upload docker image to [DockerHub](https://hub.docker.com/u/usdotjpoode)
        - [ ] Tag docker image with the version number of the app. (e.g. 1.0.0)
        - [ ] Tag docker image with year and quarter of release. (e.g. 2024-Q2)
        - [ ] Tag docker image with 'latest' tag for the most recent release.
    - [ ] Merge master branch into develop branch & verify that CI/CD passes.

### 4. DockerHub Image Testing
    - [ ] image starts up correctly
    - [ ] ProcessedMap, ProcessedSpat, OdeMap, OdeTim, OdeRawEncodedTim, OdeBsm messages are consumed
    - [ ] A single message is output for each of the input types


## jpo-cvmanager
### Prerequisites
    - [ ] jpo-ode release completed
    - [ ] jpo-conflictmonitor release completed
    - [ ] jpo-geojsonconverter release completed
    - [ ] asn1_codec release completed
    - [ ] jpo-utils release completed

#### Dependency Types
| Dependency | Type |
| --- | --- |
| jpo-ode | Maven |
| jpo-geojsonconverter | Maven |
| jpo-conflictmonitor | Maven |
| asn1_codec | Git Submodule |
| jpo-utils | Git Submodule |

### 1. Code Ready & Release Notes
    - [ ] Release notes drafted & added to `Release_notes.md` file in `docs` directory
    - [ ] Code changes for release are merged into `develop`
    - [ ] A new branch `release/(year)-(quarter)` is created from `develop`

### 2. Preliminary Testing
    - [ ] code compiles
    - [ ] unit tests pass
        - [ ] python api
        - [ ] webapp
        - [ ] intersection api
    - [ ] program starts up correctly
    - [ ] webapp can be signed into successfully
    - [ ] map page displays RSUs status information, RSU filtering, and displaying message counts in side window + heatmap
    - [ ] map page displays work zones (wzdx), intersection locations, and supports historic BSM queries
    - [ ] intersection map displays live and historic data
    - [ ] intersection dashboard displays notifications and assessments

### 3. Project Reference Updates & Release Creation
    - [ ] Merge `release/(year)-(quarter)` branch into `master` branch for the jpo-cvmanager project, and create a release with the version number of the release. (e.g. jpo-cvmanager-x.x.x)
    - [ ] Merge master branch into develop branch & verify that CI/CD passes.

## jpo-s3-deposit
### Prerequisites
None

### 1. Code Ready & Release Notes
    - [ ] Release notes drafted & added to `Release_notes.md` file in `docs` directory
    - [ ] Code changes for release are merged into `develop`
    - [ ] A new branch `release/(year)-(quarter)` is created from `develop`

### 2. Preliminary Testing
    - [ ] code compiles
    - [ ] program starts up correctly
    - [ ] deposits can be made to one of the destinations successfully

### 3. Project Reference Updates & Release Creation
    - [ ] Update version number in pom.xml if not already done
    - [ ] Merge ‘release/(year)-(quarter)’ branch into ‘master’ branch for the jpo-s3-deposit project
    - [ ] Create a release for the jpo-s3-deposit project from the ‘master’ branch and tag the release with the version number of the release. (e.g. jpo-s3-deposit-x.x.x)
    - [ ] Create docker image
        - [ ] Use the release for the jpo-s3-deposit to produce docker image with the same tag name, containing the version number.
        - [ ] Upload docker image to [DockerHub](https://hub.docker.com/u/usdotjpoode)
        - [ ] Tag docker image with the version number of the app. (e.g. 1.0.0)
        - [ ] Tag docker image with year and quarter of release. (e.g. 2024-Q2)
        - [ ] Tag docker image with 'latest' tag for the most recent release.
    - [ ] Merge master branch into develop branch & verify that CI/CD passes.

### 4. DockerHub Image Testing
    - [ ] image starts up correctly
    - [ ] deposits can be made to one of the destinations successfully


...

At this point the quarterly release process is complete.
