aem-hal-browser Integration Tests
=================================

This project allows running integration tests that exercise the capabilities of AEM via HTTP calls to its API. The integration tests use the [AEM Testing Clients][aem-testing-clients] and showcase some recommended [best practices][aem-testing-clients-best-pratices] to be put in use when writing integration tests for AEM.

To run the integration tests, run:

```
mvn clean verify -Plocal
```

_Please note: The integration tests may fail when run with AEM Dispatcher or in AEM as a Cloud Service and configured Sling Mapping, because the testing AEM testing clients currently do not support rewirting the Publish URLs respecting the Sling Mappping configuration ([#39](https://github.com/adobe/aem-testing-clients/issues/39))._


[aem-testing-clients]: https://github.com/adobe/aem-testing-clients
[aem-testing-clients-best-pratices]: https://github.com/adobe/aem-testing-clients/wiki/Best-practices
