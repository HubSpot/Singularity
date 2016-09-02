export const TOGGLE_LOCAL_REQUEST_STAR = 'TOGGLE_LOCAL_REQUEST_STAR';

// This is ONLY for users without Singularity auth configured
export const ToggleLocalRequestStar = (requestId) => {
  return (dispatch) => {
    let starredRequests = JSON.parse(localStorage.getItem('starredRequests')) || [];
    if (_.contains(starredRequests, requestId)) {
      starredRequests = _.without(starredRequests, requestId);
    } else {
      starredRequests.push(requestId);
    }
    localStorage.setItem('starredRequests', JSON.stringify(starredRequests));

    dispatch({
      type: TOGGLE_LOCAL_REQUEST_STAR,
      value: starredRequests
    });
  };
};
