# pact-compatibility-suite
Set of BDD style tests to check compatibility between Pact implementations.

This repository contains the BDD features for verifying a Pact implementation. It requires the [Cucumber BDD](https://cucumber.io/) test tool to execute.

## Adding it to a project
The easiest way to add the suite to a project to create a compatibility-suite subdirectory and then use the Git subtree command to pull the features and fixtures.
The project then needs the steps to be implemented to get the features to pass.

Recommend project layout:

```
compatibility-suite
    pact-compatibility-suite (subtree from this repo)
    steps (code for the steps, can be named anything)
```

For examples of how this has been implemented, see https://github.com/pact-foundation/pact-reference/tree/master/compatibility-suite and https://github.com/pact-foundation/pact-jvm/tree/master/compatibility-suite.

## Fixtures

The project has a number of fixture files that the features refer to. These files have the folowing formats.

### Body contents (XML)
Any file ending in `-body.xml` contains data to setup the contents of a request, response or messages. It can contain the following elements.

#### body
This is the root element.

#### body/contentType
This sets the content type of the body. It must be a valid MIME type. If not provided, it will default to either `text/plain` or `application/octet-stream`.

#### body/contents
The contents of the body. If newlines are required to be preserved, wrap the contents in a CDATA block. If the contents require the line endings to be CRLF 
(for instance, MIME multipart formats require CRLF line endings), set the attrribute `eol="CRLF"`.

### Matcher fragments
Any JSON file with a pattern `[matcher]-matcher-[type]-[format].json` or `[matcher]-matcher-[format].json` (i.e. `regex-matcher-header-v2.json`) contains matching rules
in format presisted in Pact files. They can be loaded and added to any request, response or message.

### All other files
All other files will be used as data for the contents of requests, responses or messages. The content type will be derived from the file extension.
