import { buildJsonApiAction } from './base';

export const RunAction = buildJsonApiAction('RUN_COMMAND', 'POST', (taskId, commandName) => {
  return {
    url: `/tasks/task/${taskId}/command`,
    body: {name: commandName}
  }
});
