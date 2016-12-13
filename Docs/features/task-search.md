#### Task Search

As of `0.5.0`, Singularity has better support for searching historical tasks. A global task search endpoint was added:

`/api/history/tasks` -> Retrieve the history sorted by startedAt for all inactive tasks.

The above endpoint as well as `/api/history/request/{requestId}/tasks` now take additonal query parameters:

- `requestId`: Optional request id to match (only for `/api/history/tasks` endpoint as it is already specified in the path for `/request/{requestId}/tasks`)
- `deployId`: Optional deploy id to match
- `host`: Optional host (slave host name) to match
- `lastTaskStatus`: Optional [`ExtendedTaskState`](../reference/api-docs/models#model-ExtendedTaskState) to match
- `startedAfter`: Optionally match only tasks started after this time (13 digit unix timestamp)
- `startedBefore`: Optionally match only tasks started before this time (13 digit unix timestamp)
- `orderDirection`: Sort direction (by `startedAt`), can be ASC or DESC, defaults to DESC (newest tasks first)
- `count`: Maximum number of items to return, defaults to 100 and has a maximum value of 1000
- `page`: Page of items to view (e.g. page 1 is the first `count` items, page 2 is the next `count` items), defaults to 1

For clusters using mysql that have a large number of tasks in the history, a relevant  configuration option of `taskHistoryQueryUsesZkFirst` has been added in the base Singularity Configuration. This option can be used to either prefer efficiency or exact ordering when searching through task history, it defaults to `false`.

- When `false` the setting will prefer correct ordering. This may require multiple database calls, since Singularity needs to determine the overall order of items base on persisted (in mysql) and non-persisted (still in zookeeper) tasks. The overall search may be less efficient, but the ordering is guranteed to be correct.

- When `true` the setting will prefer efficiency. In this case, it will be assumed that all task histories in zookeeper (not yet persisted) come before those in mysql (persisted). This results in faster results and fewer queries, but ordering is not guaranteed to be correct between persisted and non-persisted items.
