import { FetchRequests } from '../../actions/api/requests';

export const SET_DASHBOARD_GROUP = 'SET_DASHBOARD_GROUP';

export const SetDashboardGroup = (group) => {
  return (dispatch) => {
    localStorage['dashboard.currentGroup'] = group;
    dispatch({
      type: SET_DASHBOARD_GROUP,
      group
    });
  };
};

export const refresh = () => (dispatch) => dispatch(FetchRequests.trigger());
