╔═ snapshots a successful verification output ═╗

Verifying a pact between TestConsumer and TestProvider
  a test interaction
    returns a response which
      has status code 200 (OK)
      includes headers
        "Content-Type" with value "application/json" (OK)
      has a matching body (OK)

╔═ snapshots interaction comments with references ═╗

Verifying a pact between TestConsumer and TestProvider
  a test interaction

  References:
    openapi:
      operationId: createUser
      tag: user
    jira:
      ticket: PROJ-123

    returns a response which
      has status code 200 (OK)
      has a matching body (OK)

╔═ snapshots warning output ═╗
         WARNING: There are no consumers to verify for provider 'TestProvider'

NOTE: Skipping publishing of verification results as the interactions have been filtered


NOTE: Skipping publishing of verification results as it has been disabled (PACT_BROKER_PUBLISH_VERIFICATION_RESULTS is not 'true')


╔═ [end of file] ═╗
