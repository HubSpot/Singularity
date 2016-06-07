// actions for the Requests page go here
export const TOGGLE_STATE_FILTER = 'TOGGLE_STATE_FILTER';

export function toggleStateFilter(stateValue) {
  return { type: TOGGLE_STATE_FILTER, value: stateValue };
}
