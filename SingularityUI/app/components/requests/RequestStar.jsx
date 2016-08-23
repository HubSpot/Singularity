import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import { FetchUserSettings, AddStarredRequests, DeleteStarredRequests } from '../../actions/api/users';
import { ToggleLocalRequestStar } from '../../actions/ui/starred';
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
  userId: PropTypes.string
};

const mapStateToProps = (state, ownProps) => ({
  starred: Utils.request.isStarred(ownProps.requestId, state),
  userId: Utils.maybe(state.api.user, ['data', 'user', 'id'])
});

const mapDispatchToProps = (dispatch) => ({
  changeStar: (requestId, userId, wasStarred) => {
    if (!userId) { // No auth, use local storage
      return dispatch(ToggleLocalRequestStar(requestId));
    }
    const action = wasStarred ? DeleteStarredRequests : AddStarredRequests;
    return dispatch(action.trigger(userId, [requestId])).then(() => dispatch(FetchUserSettings.trigger(userId)));
  }
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(RequestStar);
