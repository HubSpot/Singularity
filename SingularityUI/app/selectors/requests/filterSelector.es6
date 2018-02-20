import { createSelector } from 'reselect';
import micromatch from 'micromatch';
import fuzzy from 'fuzzy';
import Utils from '../../utils';

const getRequests = (state) => state.requestsInState;
const getFilter = (state) => state.filter;
const getUtilizations = (state) => state.requestUtilizations;

export default createSelector([getRequests, getFilter, getUtilizations], (requests, filter, utilizations) => {
  let filteredRequests = requests;

  // Filter by group
  if (filter.group && filter.group !== 'all') {
    filteredRequests = _.filter(filteredRequests, (request) => Utils.maybe(request, ['request', 'group']) === filter.group);
  }

  // Filter by state
  let stateFilter = null;
  switch (filter.state) {
    case 'activeDeploy':
      stateFilter = (requestParent) => requestParent.hasActiveDeploy;
      break;
    case 'noDeploy':
      stateFilter = (requestParent) => !requestParent.hasActiveDeploy;
      break;
    case 'overUtilizedCpu':
      stateFilter = (requestParent) => {
        const utilization = _.find(utilizations, (util) => util.requestId === requestParent.request.id);
        return !!(utilization && utilization.cpuUsed > utilization.cpuReserved);
      };
      break;
    case 'underUtilizedCpu':
      stateFilter = (requestParent) => {
        const utilization = _.find(utilizations, (util) => util.requestId === requestParent.request.id);
        return !!(utilization && utilization.cpuUsed < utilization.cpuReserved);
      };
      break;
    case 'underUtilizedMem':
      stateFilter = (requestParent) => {
        const utilization = _.find(utilizations, (util) => util.requestId === requestParent.request.id);
        return !!(utilization && utilization.memBytesUsed < utilization.memBytesReserved);
      };
      break;
    case 'underUtilizedDisk':
      stateFilter = (requestParent) => {
        const utilization = _.find(utilizations, (util) => util.requestId === requestParent.request.id);
        return !!(utilization && utilization.diskBytesUsed < utilization.diskBytesReserved);
      };
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
    const getId = (requestParent) => requestParent.id || '';
    const getUser = (requestParent) => requestParent.hasActiveDeploy && requestParent.requestDeployState.activeDeploy.user || '';

    if (Utils.isGlobFilter(filter.searchFilter)) {
      const userMatches = _.filter(filteredRequests, (requestParent) => (
        micromatch.isMatch(getUser(requestParent), `${filter.searchFilter}*`)
      ));
      const idMatches = _.filter(filteredRequests, (requestParent) => (
        micromatch.isMatch(getId(requestParent), `${filter.searchFilter}*`)
      ));
      filteredRequests = _.union(userMatches, idMatches);
    } else {
      const userMatches = fuzzy.filter(filter.searchFilter, filteredRequests, {
        extract: getUser
      });
      // Allow searching by the first letter of each word by applying same
      // search heuristics to just the upper case characters of each option
      const idMatches = fuzzy.filter(filter.searchFilter, filteredRequests, {
        extract: Utils.isAllUpperCase(filter.searchFilter)
          ? (requestParent) => Utils.getUpperCaseCharacters(getId(requestParent))
          : getId,
      });
      filteredRequests = Utils.fuzzyFilter(filter.searchFilter, _.union(userMatches, idMatches));
    }
  }

  return filteredRequests;
});
