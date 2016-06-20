import buildApiAction from './base';

const POST_JSON = {method: 'POST', headers: {'Accept': 'application/json', 'Content-Type': 'application/json'}};

export const RunAction = buildApiAction('RUN_COMMAND',
                                        (taskId, commandName) => `/tasks/task/${taskId}/command`,
                                        (taskId, commandName) => _.extend({}, POST_JSON, {body: JSON.stringify({name: commandName})}));
