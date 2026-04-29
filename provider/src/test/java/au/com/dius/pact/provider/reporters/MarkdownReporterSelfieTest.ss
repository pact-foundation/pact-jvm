╔═ snapshots a failed verification report ═╗
# TestProvider

| Description    | Value |
| -------------- | ----- |
| Date Generated | <timestamp> |
| Pact Version   | <version> |

## Summary

| Consumer    | Result |
| ----------- | ------ |
| TestConsumer | Failed |

## Verifying a pact between _TestConsumer_ and _TestProvider_

a test interaction  <br/>
&nbsp;&nbsp;returns a response which  <br/>
&nbsp;&nbsp;&nbsp;&nbsp;has status code **200** (<span style='color:red'>FAILED</span>)

```
expected 201 but was 200
```

&nbsp;&nbsp;&nbsp;&nbsp;includes headers  <br/>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"**Content-Type**" with value "**[application/json]**" (<span style='color:red'>FAILED</span>)  

```
Expected 'application/json' but received 'text/plain'
```

&nbsp;&nbsp;&nbsp;&nbsp;has a matching body (<span style='color:red'>FAILED</span>)  

| Path | Failure |
| ---- | ------- |
|`$.name`|Expected "expected" but received "actual"|


Diff:

```diff
- expected
+ actual
```



╔═ snapshots a pending consumer verification report ═╗
# TestProvider

| Description    | Value |
| -------------- | ----- |
| Date Generated | <timestamp> |
| Pact Version   | <version> |

## Summary

| Consumer    | Result |
| ----------- | ------ |
| PendingConsumer [Pending] | OK |

## Verifying a pact between _PendingConsumer_ and _TestProvider_ for tag main [PENDING]

a pending interaction  <br/>

╔═ snapshots a successful verification report ═╗
# TestProvider

| Description    | Value |
| -------------- | ----- |
| Date Generated | <timestamp> |
| Pact Version   | <version> |

## Summary

| Consumer    | Result |
| ----------- | ------ |
| TestConsumer | OK |

## Verifying a pact between _TestConsumer_ and _TestProvider_

a test interaction  <br/>
&nbsp;&nbsp;returns a response which  <br/>
&nbsp;&nbsp;&nbsp;&nbsp;has status code **200** (<span style='color:green'>OK</span>)  <br/>
&nbsp;&nbsp;&nbsp;&nbsp;includes headers  <br/>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"**Content-Type**" with value "**[application/json]**" (<span style='color:green'>OK</span>)  <br/>
&nbsp;&nbsp;&nbsp;&nbsp;has a matching body (<span style='color:green'>OK</span>)  <br/>

╔═ snapshots interaction comments with references ═╗
# TestProvider

| Description    | Value |
| -------------- | ----- |
| Date Generated | <timestamp> |
| Pact Version   | <version> |

## Summary

| Consumer    | Result |
| ----------- | ------ |
| TestConsumer | OK |

## Verifying a pact between _TestConsumer_ and _TestProvider_

a test interaction  <br/>
References:
* openapi:
  * operationId: createUser
  * tag: user
* jira:
  * ticket: PROJ-123
&nbsp;&nbsp;returns a response which  <br/>
&nbsp;&nbsp;&nbsp;&nbsp;has status code **200** (<span style='color:green'>OK</span>)  <br/>
&nbsp;&nbsp;&nbsp;&nbsp;has a matching body (<span style='color:green'>OK</span>)  <br/>

╔═ [end of file] ═╗
