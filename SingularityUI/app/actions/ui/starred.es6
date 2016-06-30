export const CHANGE_REQUEST_STAR = 'CHANGE_REQUEST_STAR';

export const toggleRequestStar = (requestId) => {

  return (dispatch) => {
    let starredRequests = JSON.parse(localStorage.getItem('starredRequests')) || [];
    if (_.contains(starredRequests, requestId)) {
      starredRequests = _.without(starredRequests, requestId);
    } else {
      starredRequests.push(requestId);
    }
    localStorage.setItem('starredRequests', JSON.stringify(starredRequests));

    dispatch({
      type: CHANGE_REQUEST_STAR,
      value: starredRequests
    });
  };
};
