export const CHANGE_REQUEST_STAR = 'CHANGE_REQUEST_STAR';

export const changeRequestStar = (requestId) => ({
  type: CHANGE_REQUEST_STAR,
  value: requestId
});
