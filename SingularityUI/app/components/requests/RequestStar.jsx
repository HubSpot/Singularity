import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import { getUserSettingsAPI } from '../../selectors/requests';
import { FetchUserSettings, UpdateUserSettings } from '../../actions/api/user';
import { UpdateTemporaryUserSettings, ClearTemporaryUserSettings } from '../../actions/ui/temporaryUserSettings';
import Utils from '../../utils';


const RequestStar = ({requestId, changeStar, starred, userId, settings}) => (
  <a className="star" data-starred={starred} onClick={() => changeStar(requestId, userId, settings)}>
    <span className="glyphicon glyphicon-star"></span>
  </a>
);

RequestStar.propTypes = {
  requestId: PropTypes.string.isRequired,
  changeStar: PropTypes.func.isRequired,
  starred: PropTypes.bool.isRequired,
  userId: PropTypes.string.isRequired,
  settings: PropTypes.object
};

const mapStateToProps = (state, ownProps) => ({
  starred: Utils.request.isStarred(ownProps.requestId, Utils.maybe(getUserSettingsAPI(state), ['data'])),
  settings: Utils.maybe(state.api.userSettings, ['data']),
  userId: Utils.maybe(state.api.user, ['data', 'user', 'id'])
});

const mapDispatchToProps = (dispatch) => ({
  changeStar: (requestId, userId, settings) => {
    if (userId) {
      const newSettings = Utils.request.toggleStar(requestId, settings);
      dispatch(UpdateTemporaryUserSettings(newSettings));
      const clearTemporaryUserSettings = () => dispatch(ClearTemporaryUserSettings());
      return dispatch(UpdateUserSettings.trigger(userId, newSettings)).then(
        () => dispatch(FetchUserSettings.trigger(userId)).then(clearTemporaryUserSettings),
        clearTemporaryUserSettings
      );
    }
    return Promise.resolve();
  }
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(RequestStar);
