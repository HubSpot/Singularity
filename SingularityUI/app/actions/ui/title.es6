import { updateTitle } from 'redux-title';

export const setTitle = (title) => (dispatch) =>
  dispatch(updateTitle(`${title} - ${config.title}`));