import { FetchSlaves, FreezeSlave, DecommissionSlave, RemoveSlave, ReactivateSlave, FetchExpiringSlaveStates } from '../../actions/api/slaves';

export const UPDATE_SLAVES_TABLE_SETTINGS = 'UPDATE_SLAVES_TABLE_SETTINGS';

export const UpdateSlavesTableSettings = (columns, paginated) => {
  return (dispatch) => {
    localStorage['slaves.columns'] = JSON.stringify(columns);
    localStorage['slaves.paginated'] = paginated;
    dispatch({
      columns: columns,
      paginated: paginated,
      type: UPDATE_SLAVES_TABLE_SETTINGS
    });
  };
};

export const refresh = () => (dispatch) =>
  Promise.all([
    dispatch(FetchSlaves.trigger()),
    dispatch(FetchExpiringSlaveStates.trigger()),
  ]);

export const initialize = () => (dispatch) =>
  Promise.all([
    dispatch(FreezeSlave.clear()),
    dispatch(DecommissionSlave.clear()),
    dispatch(RemoveSlave.clear()),
    dispatch(ReactivateSlave.clear())
  ]).then(() => dispatch(refresh()));
