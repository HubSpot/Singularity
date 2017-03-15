import rootComponent from '../../rootComponent';

import LogContainer from './LogContainer';

import { initialize, initializeUsingActiveTasks } from '../../actions/log';
import { updateActiveTasks } from '../../actions/activeTasks';

const refreshTail = (props) => (dispatch) => {
  const splits = props.params.taskId.split('-');
  const requestId = splits.slice(0, splits.length - 5).join('-');
  const search = props.location.query.search || '';
  const path = props.params.splat.replace(props.params.taskId, '$TASK_ID');
  const initPromise = dispatch(initialize(requestId, path, search, [props.params.taskId], props.location.query.viewMode || 'split'));
  return initPromise.then(() => {
    dispatch(updateActiveTasks(requestId));
  });
};

export const Tail = rootComponent(LogContainer, refreshTail, false, false);

const refreshAggregateTail = (props) => (dispatch) => {
  const viewMode = props.location.query.viewMode || 'split';
  const search = props.location.query.search || '';
  const taskIds = props.location.query.taskIds;

  let initPromise;
  if (taskIds) {
    initPromise = dispatch(initialize(props.params.requestId, props.params.splat, search, taskIds.split(','), viewMode));
  } else {
    initPromise = dispatch(initializeUsingActiveTasks(props.params.requestId, props.params.splat, search, viewMode));
  }
  initPromise.then(() => {
    dispatch(updateActiveTasks(props.params.requestId));
  });
}

export const AggregateTail = rootComponent(LogContainer, refreshAggregateTail, false, false);
