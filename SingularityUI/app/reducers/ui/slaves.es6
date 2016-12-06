import * as SlavesActions from '../../actions/ui/slaves';
import Utils from '../../utils';

const initialState = {
  columns: window.localStorage.hasOwnProperty('slaves.columns')
    ? JSON.parse(window.localStorage.getItem('slaves.columns'))
    : Utils.DEFAULT_SLAVES_COLUMNS,
  paginated: window.localStorage.hasOwnProperty('slaves.paginated')
    ? (localStorage.getItem('slaves.paginated') == "true")
    : true
};

export default (state = initialState, action) => {
  if (action.type === SlavesActions.UPDATE_SLAVES_TABLE_SETTINGS) {
    return {
      columns: action.columns,
      paginated: action.paginated
    };
  } else {
    return state;
  }
};
