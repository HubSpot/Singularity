import { createSelector } from 'reselect';
import micromatch from 'micromatch';
import fuzzy from 'fuzzy';
import Utils from '../../utils';

const getRequests = (state) => state.requestsInState;
const getFilter = (state) => state.filter;

export default createSelector([getRequests, getFilter], (requests, filter) => {
  let filteredRequests = requests;

  // Filter by state
  let stateFilter = null;
  switch (filter.state) {
    case 'activeDeploy':
      stateFilter = (requestParent) => requestParent.hasActiveDeploy;
      break;
    case 'noDeploy':
      stateFilter = (requestParent) => !requestParent.hasActiveDeploy;
      break;
    default:
      break;
  }
  if (stateFilter) {
    filteredRequests = _.filter(filteredRequests, stateFilter);
  }

  // Filter by request type
  if (!_.contains(['pending', 'cleanup'], filter.type)) {
    filteredRequests = _.filter(filteredRequests, (requestParent) => requestParent.request && _.contains(filter.subFilter, requestParent.request.requestType));
  }

  // Filter by glob or fuzzy string
  if (filter.searchFilter) {
    const id = {extract: (requestParent) => requestParent.id || ''};
    const user = {extract: (requestParent) => `${requestParent.hasActiveDeploy ? requestParent.requestDeployState.activeDeploy.user : ''}`};

    if (Utils.isGlobFilter(filter.searchFilter)) {
      const res1 = _.filter(filteredRequests, (requestParent) => {
        return micromatch.any(user.extract(requestParent), `${filter.searchFilter}*`);
      });
      const res2 = _.filter(filteredRequests, (requestParent) => {
        return micromatch.any(id.extract(requestParent), `${filter.searchFilter}*`);
      });
      filteredRequests = _.union(res1, res2).reverse();
    } else {
      _.each(filteredRequests, (requestParent) => {requestParent.id = id.extract(requestParent);});
      const res1 = fuzzy.filter(filter.searchFilter, filteredRequests, user);
      const res2 = fuzzy.filter(filter.searchFilter, filteredRequests, id);
      filteredRequests = Utils.fuzzyFilter(filter.searchFilter, _.union(res1, res2));
    }
  }

  return filteredRequests;
});
