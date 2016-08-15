import { createSelector } from 'reselect';
import micromatch from 'micromatch';
import Utils from '../../utils';

const getRequests = (state) => state.requestsInState;
const getFilter = (state) => state.filter;

export default createSelector([getRequests, getFilter], (requests, filter) => {
  let filteredRequests = requests;

  // Filter by state
  let stateFilter = null;
  switch (filter.state) {
    case 'activeDeploy':
      stateFilter = (r) => r.hasActiveDeploy;
      break;
    case 'noDeploy':
      stateFilter = (r) => !r.hasActiveDeploy;
      break;
    default:
      break;
  }
  if (stateFilter) {
    filteredRequests = _.filter(filteredRequests, stateFilter);
  }

  // Filter by request type
  if (!_.contains(['pending', 'cleanup'], filter.type)) {
    filteredRequests = _.filter(filteredRequests, (r) => r.request && _.contains(filter.subFilter, r.request.requestType));
  }

  // Filter by glob or string match
  if (filter.searchFilter) {
    const id = (r) => r.id || '';
    const user = (r) => `${r.hasActiveDeploy ? r.requestDeployState.activeDeploy.user : ''}`;

    if (Utils.isGlobFilter(filter.searchFilter)) {
      const res1 = _.filter(filteredRequests, (request) => {
        return micromatch.any(user(request).toLowerCase(), `*${filter.searchFilter.toLowerCase()}*`);
      });
      const res2 = _.filter(filteredRequests, (request) => {
        return micromatch.any(id(request).toLowerCase(), `*${filter.searchFilter.toLowerCase()}*`);
      });
      filteredRequests = _.sortBy(_.union(res1, res2), (request) => (micromatch.any(id(request).toLowerCase(), `${filter.searchFilter.toLowerCase()}*`) ? 1 : 0)).reverse();
    } else {
      const res1 = _.filter(filteredRequests, request => id(request).toLowerCase().indexOf(filter.searchFilter.toLowerCase()) > -1);
      const res2 = _.filter(filteredRequests, request => user(request).toLowerCase().indexOf(filter.searchFilter.toLowerCase()) > -1);
      filteredRequests = _.uniq(_.sortBy(_.union(res1, res2), (request) => (id(request).toLowerCase().startsWith(filter.searchFilter.toLowerCase()) ? 1 : 0)).reverse());
    }
  }

  return filteredRequests;
});
