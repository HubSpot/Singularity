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
