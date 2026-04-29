╔═ snapshots a successful verification log output ═╗
INFO Verifying a pact between TestConsumer and TestProvider
INFO   a test interaction
INFO     returns a response which
INFO       has status code 200 (OK)
INFO       includes headers
INFO         "Content-Type" with value "application/json" (OK)
INFO       has a matching body (OK)
╔═ snapshots interaction comments with references ═╗
INFO Verifying a pact between TestConsumer and TestProvider
INFO   a test interaction
INFO 
  References:
    openapi:
      operationId: createUser
      tag: user
    jira:
      ticket: PROJ-123

INFO     returns a response which
INFO       has status code 200 (OK)
INFO       has a matching body (OK)
╔═ snapshots warning log output ═╗
WARN          There are no consumers to verify for provider 'TestProvider'
WARN NOTE: Skipping publishing of verification results as the interactions have been filtered
WARN NOTE: Skipping publishing of verification results as it has been disabled (PACT_BROKER_PUBLISH_VERIFICATION_RESULTS is not 'true')
╔═ [end of file] ═╗
