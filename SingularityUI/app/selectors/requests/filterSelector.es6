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
      stateFilter = (r) => r.hasActiveDeploy
      break;
    case 'noDeploy':
      stateFilter = (r) => !r.hasActiveDeploy
      break;
  }
  if (stateFilter) {
    filteredRequests = _.filter(filteredRequests, stateFilter);
  }

  // Filter by request type
  if (!_.contains(['pending', 'cleanup'], filter.type)) {
    filteredRequests = _.filter(filteredRequests, (r) => r.request && _.contains(filter.subFilter, r.request.requestType));
  }

  // Filter by glob or fuzzy string
  if (filter.searchFilter) {
    const id = {extract: (r) => r.id || ''};
    const user = {extract: (r) => `${r.hasActiveDeploy ? r.requestDeployState.activeDeploy.user : ''}`};

    if (Utils.isGlobFilter(filter.searchFilter)) {
      let res1 = _.filter(filteredRequests, (request) => {
        return micromatch.any(user.extract(request), filter.searchFilter + '*');
      });
      let res2 = _.filter(filteredRequests, (request) => {
        return micromatch.any(id.extract(request), filter.searchFilter + '*');
      });
      filteredRequests = _.union(res1, res2).reverse();
    } else {
      _.each(filteredRequests, (r) => r.id = id.extract(r));
      let res1 = fuzzy.filter(filter.searchFilter, filteredRequests, user);
      let res2 = fuzzy.filter(filter.searchFilter, filteredRequests, id);
      filteredRequests = _.uniq(_.pluck(_.sortBy(_.union(res1, res2), (t) => Utils.fuzzyAdjustScore(filter.searchFilter, t)), 'original').reverse());
    }
  }

  return filteredRequests;
});
