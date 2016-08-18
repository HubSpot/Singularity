export const UPDATE_TEMPORARY_USER_SETTINGS = 'UPDATE_TEMPORARY_USER_SETTINGS';
export const CLEAR_TEMPORARY_USER_SETTINGS = 'CLEAR_TEMPORARY_USER_SETTINGS';

// Use this to show updated settings locally while waiting for an update
// settings API call to go through
export const UpdateTemporaryUserSettings = (newSettings) => {
  return (dispatch) => {
    dispatch({
      newSettings,
      type: UPDATE_TEMPORARY_USER_SETTINGS
    });
  };
};

export const ClearTemporaryUserSettings = () => {
  return (dispatch) => {
    dispatch({
      type: CLEAR_TEMPORARY_USER_SETTINGS
    });
  };
};
