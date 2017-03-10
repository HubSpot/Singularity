import { ClearPriorityFreeze } from '../../actions/api/disasters';

const clearPriorityFreeze = (state, action) => {
  if (action.type === 'CLEAR_PRIORITY_FREEZE') {
    return _.assign({}, state, {data: []});
  } else {
    return state;
  }
};

export default clearPriorityFreeze;
