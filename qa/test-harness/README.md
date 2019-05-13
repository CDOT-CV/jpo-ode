# ODE Regression Test Harness

## Overview

This folder contains the automated regression test harness application built for the ODE. The test harness sends a file to the ODE, listens for messages to be published to Kafka, then validates the output.

## Requirements

- Python 3.6+

## Installation

This test harness is a python script designed for Python 3.6+. Using virtualenv with Python 3.7 will give the most consistent experience.

#### Option 1: (Recommended) Using python3 with virtualenv

1. [Install python3](https://realpython.com/installing-python/)
2. [Install virtualenv](https://virtualenv.pypa.io/en/stable/installation/)
3. Create a virtualenv repository:
```
virtualenv -p python3 testharness
```
4. Activate virtualenv:
```
source ./testharness/bin/activate
```
5. Install the required python modules using pip:
```
pip install -r requirements.txt
```
5. _Run the test script using the **Usage** instructions below._
6. Deactivate virtualenv
```
deactivate
```

#### Option 2: Using system python

1. Install the required python modules using `pip install -r requirements.txt`.
2.  _Run the test script using the **Usage** instructions below._

## Usage

**Important Note**: Before you begin either of the options below, you must set the [DOCKER_HOST_IP](https://github.com/usdot-jpo-ode/jpo-ode/wiki/Docker-management#obtaining-docker_host_ip) environment variable.

### Option 1:  (Recommended) Using the pre-configured test script

You may perform a complete test of all of the ODE input file types by running the pre-configured `full-test-sample` script.
However, you may need to modify the `--ode-upload-url` to match your specific ODE deployment. It is also strongly recommended
to use your own test files and add additional tests to ensure a better a fuller coverage. To do so, copy the provided
`full-test-sample` into a new file name such as `full-test.sh` and make all necessary and recommended modification to the new file.
You may then run your specific and customized tests by running the following command.

`source ./full-test.sh` OR `/bin/sh ./full-test.sh`

### Option 2: Using custom test cases

See the [odevalidator library](https://github.com/usdot-jpo-ode/ode-output-validator-library) for more information on how to create your own test case configurations.
[odevalidator library](https://github.com/usdot-jpo-ode/ode-output-validator-library) is a submodule of `test-harness`. Even though, it can be
pull from GitHub, the `test-harness` will not be able to use the included config file. 
it is recommended to update the submodule to allow the `test-harness` to use the latest config file, not to mention
the latest [odevalidator library](https://github.com/usdot-jpo-ode/ode-output-validator-library) code if local changes are present.

You may run custom test cases by creating your own configuration file by setting the following command-line arguments:

`python test-harness.py --config-file <CONFIGFILEPATH> --data-file <DATAFILEPATH> --ode-upload-url <ODEUPLOADURL> --kafka-topics <KAFKATOPICS> --output-file <LOGFILEPATH>`

```
usage: test-harness.py [-h] --data-file DATAFILEPATH --ode-upload-url <ODEUPLOADURL> --kafka-topics KAFKATOPICS
                       [--config-file CONFIGFILEPATH]
                       [--output-file LOGFILEPATH]

optional arguments:
  -h, --help            show this help message and exit
  --data-file DATAFILEPATH
                        Path to log data file that will be sent to the ODE for
                        validation.
  --ode-upload-url ODEUPLOADURL
						Full URL of the ODE upload directory to which the data-file will be sent, 
						e.g. https://ode.io:8443/upload/bsmlog.
  --kafka-topics KAFKATOPICS
                        Comma-separated list of Kafka topics to which to the test harness should listen
                        for output messages.
  --config-file CONFIGFILEPATH
                        [Optional] Path to ini configuration file used for testing.
						If not specified, the test-harness will use the default config file included in odevalidator package
  --output-file LOGFILEPATH
                        [Optional] Output file to which detailed validation results will
                        be printed.
```

## Release History

### 2019-05-09
* Integrated [odevalidator v0.0.4](https://github.com/usdot-jpo-ode/ode-output-validator-library/releases/tag/odevalidator-0.0.4)

### 2019-04-15
* Added ode-upload-url command line argument so the test harness no longer 
relies on DOCKER_HOST_IP environment variable to reach the ODE. 
* Renamed `full-test.sh` to `full-test-sample.sh`. The users must create their own `full-test.sh` according to the provided sample script.

### 2019-04-09
* Integrated [odevalidator-0.0.3](https://github.com/usdot-jpo-ode/ode-output-validator-library/releases/tag/odevalidator-0.0.3)

### 2019-04-05
* Integrated [odevalidator-0.0.2](https://github.com/usdot-jpo-ode/ode-output-validator-library/releases/tag/odevalidator-0.0.2)

### 2019-04-01
* Integrated initial release of the validator library [odevalidator-v0.0.1](https://github.com/usdot-jpo-ode/ode-output-validator-library/releases/tag/odevalidator-0.0.1)