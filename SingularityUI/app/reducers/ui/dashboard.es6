import * as DashboardActions from '../../actions/ui/dashboard';
import { FetchUser } from '../../actions/api/auth';
import Utils from '../../utils';

const initialState = {
  currentGroup: localStorage['dashboard.currentGroup']
};

export default (state = initialState, action) => {
  switch (action.type) {
    case DashboardActions.SET_DASHBOARD_GROUP:
      return {
        currentGroup: action.group
      };
    case FetchUser.SUCCESS:
      if (state.currentGroup === undefined) {
        return {
          currentGroup: (Utils.maybe(action, ['data', 'user', 'groups']) || [])[0]
        }
      }
      return state;
    default:
      return state;
  }
};
