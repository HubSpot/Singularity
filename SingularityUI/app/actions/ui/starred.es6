export const GET_STARRED_REQUESTS = 'GET_STARRED_REQUESTS';

export const getStarredRequests = () => ({
  type: GET_STARRED_REQUESTS
});

export const CHANGE_REQUEST_STAR = 'CHANGE_REQUEST_STAR';

export const changeRequestStar = (requestId) => ({
  type: CHANGE_REQUEST_STAR,
  value: requestId
});
