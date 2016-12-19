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
