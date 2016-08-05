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
      stateFilter = (request) => request.hasActiveDeploy;
      break;
    case 'noDeploy':
      stateFilter = (request) => !request.hasActiveDeploy;
      break;
    default:
      break;
  }
  if (stateFilter) {
    filteredRequests = _.filter(filteredRequests, stateFilter);
  }

  // Filter by request type
  if (!_.contains(['pending', 'cleanup'], filter.type)) {
    filteredRequests = _.filter(filteredRequests, (request) => request.request && _.contains(filter.subFilter, request.request.requestType));
  }

  // Filter by glob or fuzzy string
  if (filter.searchFilter) {
    const id = {extract: (request) => request.id || ''};
    const user = {extract: (request) => `${request.hasActiveDeploy ? request.requestDeployState.activeDeploy.user : ''}`};

    if (Utils.isGlobFilter(filter.searchFilter)) {
      const res1 = _.filter(filteredRequests, (request) => {
        return micromatch.any(user.extract(request), `${filter.searchFilter}*`);
      });
      const res2 = _.filter(filteredRequests, (request) => {
        return micromatch.any(id.extract(request), `${filter.searchFilter}*`);
      });
      filteredRequests = _.union(res1, res2).reverse();
    } else {
      _.each(filteredRequests, (request) => {request.id = id.extract(request);});
      const res1 = fuzzy.filter(filter.searchFilter, filteredRequests, user);
      const res2 = fuzzy.filter(filter.searchFilter, filteredRequests, id);
      filteredRequests = _.uniq(_.pluck(_.sortBy(_.union(res1, res2), (task) => Utils.fuzzyAdjustScore(filter.searchFilter, task)), 'original').reverse());
    }
  }

  return filteredRequests;
});
