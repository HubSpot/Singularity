import { createSelector } from 'reselect';
import micromatch from 'micromatch';
import fuzzy from 'fuzzy';
import Utils from '../../utils';

const getTasks = (state) => state.tasks;
const getFilter = (state) => ({state: state.filter.taskStatus, requestTypes: state.filter.requestTypes, filterText: state.filter.filterText});

export default createSelector([getTasks, getFilter], (tasks, filter) => {

  // Filter by requestType
  if (filter.state === 'active') {
    tasks = _.filter(tasks, (task) => {
      return task.taskRequest && _.contains(filter.requestTypes, task.taskRequest.request.requestType);
    });
  }

  // Filter by glob or fuzzy string
  if (filter.filterText) {
    const host = {extract: (t) => `${t.taskId && t.taskId.host}`};
    const id = {extract: (t) => `${t.taskId ? t.taskId.id : t.pendingTask.pendingTaskId.id}`};
    const rack = {extract: (t) => `${t.taskId && t.taskId.rackId}`};

    if (Utils.isGlobFilter(filter.filterText)) {
      let res1 = _.filter(tasks, (task) => {
        return micromatch.any(host.extract(task), filter.filterText + '*');
      });
      let res2 = _.filter(tasks, (task) => {
        return micromatch.any(id.extract(task), filter.filterText + '*');
      });
      let res3 = _.filter(tasks, (task) => {
        return micromatch.any(rack.extract(task), filter.filterText + '*');
      });
      tasks = _.union(res1, res2, res3).reverse();
    } else {
      _.each(tasks, (t) => t.id = id.extract(t));
      let res1 = fuzzy.filter(filter.filterText, tasks, host);
      let res2 = fuzzy.filter(filter.filterText, tasks, id);
      let res3 = fuzzy.filter(filter.filterText, tasks, rack);
      tasks = _.uniq(_.pluck(_.sortBy(_.union(res3, res1, res2), (t) => Utils.fuzzyAdjustScore(filter.filterText, t)), 'original').reverse());
    }
  }

  return tasks;
});
