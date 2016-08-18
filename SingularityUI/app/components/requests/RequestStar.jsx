import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import { getUserSettingsAPI } from '../../selectors/requests';
import { FetchUserSettings, AddStarredRequests, DeleteStarredRequests } from '../../actions/api/user';
import { UpdateTemporaryUserSettings, ClearTemporaryUserSettings } from '../../actions/ui/temporaryUserSettings';
import Utils from '../../utils';
import classNames from 'classnames';


const RequestStar = ({requestId, changeStar, starred, userId, settings}) => (
  <a className={classNames('star', {starred})} onClick={() => changeStar(requestId, userId, settings)}>
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
  settings: Utils.maybe(getUserSettingsAPI(state), ['data']),
  userId: Utils.maybe(state.api.user, ['data', 'user', 'id'])
});

const mapDispatchToProps = (dispatch) => ({
  changeStar: (requestId, userId, settings) => {
    if (userId) {
      let temporaryUserSettings;
      let promise;
      if (Utils.request.isStarred(requestId, settings)) {
        temporaryUserSettings = Utils.request.removeStar(requestId, settings);
        promise = dispatch(DeleteStarredRequests.trigger(userId, [requestId]));
      } else {
        temporaryUserSettings = Utils.request.addStar(requestId, settings);
        promise = dispatch(AddStarredRequests.trigger(userId, [requestId]));
      }
      dispatch(UpdateTemporaryUserSettings(temporaryUserSettings));
      return promise.then(
        () => dispatch(FetchUserSettings.trigger(userId)).then(() => dispatch(ClearTemporaryUserSettings())),
        () => dispatch(ClearTemporaryUserSettings())
      );
    }
    return Promise.resolve();
  }
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(RequestStar);
