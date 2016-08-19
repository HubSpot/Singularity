import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import { getUserSettingsAPI } from '../../selectors/requests';
import { FetchUserSettings, AddStarredRequests, DeleteStarredRequests } from '../../actions/api/user';
import Utils from '../../utils';
import classNames from 'classnames';


const RequestStar = ({requestId, changeStar, starred, userId}) => (
  <a className={classNames('star', { starred })} onClick={() => changeStar(requestId, userId, starred)}>
    <span className="glyphicon glyphicon-star"></span>
  </a>
);

RequestStar.propTypes = {
  requestId: PropTypes.string.isRequired,
  changeStar: PropTypes.func.isRequired,
  starred: PropTypes.bool.isRequired,
  userId: PropTypes.string.isRequired
};

const mapStateToProps = (state, ownProps) => ({
  starred: Utils.request.isStarred(ownProps.requestId, Utils.maybe(getUserSettingsAPI(state), ['data']), state.temporaryStars),
  userId: Utils.maybe(state.api.user, ['data', 'user', 'id'])
});

const mapDispatchToProps = (dispatch) => ({
  changeStar: (requestId, userId, wasStarred) => {
    if (!userId) return Promise.resolve();
    const action = wasStarred ? DeleteStarredRequests : AddStarredRequests;
    return dispatch(action.trigger(userId, [requestId])).then(() => dispatch(FetchUserSettings.trigger(userId)));
  }
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(RequestStar);
