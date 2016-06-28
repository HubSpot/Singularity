// actions for the Requests page go here
export const TOGGLE_STATE_FILTER = 'TOGGLE_STATE_FILTER';

export function toggleStateFilter(stateValue) {
  return { type: TOGGLE_STATE_FILTER, value: stateValue };
}

export const CHANGE_TYPE_FILTER = 'CHANGE_TYPE_FILTER';

export function changeTypeFilter(typeValue) {
  return { type: CHANGE_TYPE_FILTER, value: typeValue };
}

export const CHANGE_TEXT_FILTER = 'CHANGE_TEXT_FILTER';

export function changeTextFilter(textValue) {
  return { type: CHANGE_TEXT_FILTER, value: textValue };
}
