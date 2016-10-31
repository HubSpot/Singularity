import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { FetchUser } from 'actions/api/auth';
import { AddStarredRequests, DeleteStarredRequests } from '../../actions/api/users';
import Utils from '../../utils';

const RequestStar = ({requestId, changeStar, starred}) => (
  <a className="star" data-starred={starred} onClick={() => changeStar(requestId, starred)}>
    <span className="glyphicon glyphicon-star"></span>
  </a>
);

RequestStar.propTypes = {
  requestId: PropTypes.string.isRequired,
  changeStar: PropTypes.func.isRequired,
  starred: PropTypes.bool.isRequired
};

const mapStateToProps = (state, ownProps) => {
  return {
    starred: _.contains(Utils.maybe(state.api.user, ['data', 'settings', 'starredRequestIds'], []), ownProps.requestId)
  };
};

const mapDispatchToProps = (dispatch) => {
  return {
    changeStar: (requestId, starred) => {
      if (starred) {
        dispatch(DeleteStarredRequests.trigger([requestId])).then(() => dispatch(FetchUser.trigger()));
      } else {
        dispatch(AddStarredRequests.trigger([requestId])).then(() => dispatch(FetchUser.trigger()));
      }
    }
  };
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(RequestStar);
