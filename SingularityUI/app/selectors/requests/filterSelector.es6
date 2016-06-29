import { createSelector } from 'reselect';
import micromatch from 'micromatch';
import fuzzy from 'fuzzy';
import Utils from '../../utils';

const getRequests = (state) => state.requests;
const getFilter = (state) => state.filter;

export default createSelector([getRequests, getFilter], (requests, filter) => {

  // Filter by requestType
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
    requests = _.filter(requests, stateFilter);
  }

  // Filter by glob or fuzzy string
  if (filter.filterText) {
    const host = {extract: (t) => `${t.taskId && t.taskId.host}`};
    const id = {extract: (t) => `${t.taskId ? t.taskId.id : t.pendingTask.pendingTaskId.id}`};
    const rack = {extract: (t) => `${t.taskId && t.taskId.rackId}`};

    if (Utils.isGlobFilter(filter.filterText)) {
      let res1 = _.filter(requests, (task) => {
        return micromatch.any(host.extract(task), filter.filterText + '*');
      });
      let res2 = _.filter(requests, (task) => {
        return micromatch.any(id.extract(task), filter.filterText + '*');
      });
      let res3 = _.filter(requests, (task) => {
        return micromatch.any(rack.extract(task), filter.filterText + '*');
      });
      requests = _.union(res1, res2, res3).reverse();
    } else {
      _.each(tasks, (t) => t.id = id.extract(t));
      let res1 = fuzzy.filter(filter.filterText, requests, host);
      let res2 = fuzzy.filter(filter.filterText, requests, id);
      let res3 = fuzzy.filter(filter.filterText, requests, rack);
      requests = _.uniq(_.pluck(_.sortBy(_.union(res3, res1, res2), (t) => Utils.fuzzyAdjustScore(filter.filterText, t)), 'original').reverse());
    }
  }

  return requests;
});
