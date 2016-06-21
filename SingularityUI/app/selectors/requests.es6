import { createSelector } from 'reselect';

const getStarred = (state) => state.ui.starred;
const getRequests = (state) => state.api.requests;

export const combineStarredWithRequests = createSelector(
  [getStarred, getRequests],
  (starredData, requestsAPI) => {
    // TODO: make this use two sorted lists
    // to combine the data
    // because this is O(awful)
    return requestsAPI.data.map((r) => {
      if (starredData.indexOf(r.request.id) > -1) {
        return {
          ...r,
          starred: true
        };
      }
      return r;
    });
  }
);
