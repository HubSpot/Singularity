import { buildJsonApiAction } from './base';

export const RunAction = buildJsonApiAction('RUN_COMMAND', (taskId, commandName) => {
  return {
    url: `/tasks/task/${taskId}/command`,
    body: {name: commandName}
  }
});
