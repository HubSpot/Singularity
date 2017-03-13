import { FetchDisabledActions, FetchDisastersData, FetchPriorityFreeze, FetchTaskCredits } from '../../actions/api/disasters';

export const refresh = () => (dispatch) => 
  Promise.all([
    dispatch(FetchDisabledActions.trigger()),
    dispatch(FetchDisastersData.trigger()),
    dispatch(FetchPriorityFreeze.trigger()),
    dispatch(FetchTaskCredits.trigger())
  ]);