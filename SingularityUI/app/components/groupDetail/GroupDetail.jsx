import React, {PropTypes, Component} from 'react';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';

import { Row, Col, Nav, NavItem, Label } from 'react-bootstrap';
import RequestDetailPage from '../requestDetail/RequestDetailPage';
import MetadataButton from '../common/MetadataButton';
import { refresh } from '../../actions/ui/groupDetail';
import ActionDropdown from './ActionDropdown';

class GroupDetail extends Component {

  constructor(props) {
    super(props);
    this.state = {
      showRequestId: _.first(props.group.requestIds)
    };
    _.bindAll(this, 'handleRequestSelect');
  }

  handleRequestSelect(eventkey) {
    this.setState({
      showRequestId: eventkey
    });
  }

  render() {
    const {group, location, requestsNotFound} = this.props;
    const metadata = !_.isEmpty(group.metadata) && (
      <MetadataButton title={group.id} metadata={group.metadata}>View Metadata</MetadataButton>
    );
    const requestPages = {};
    group.requestIds.forEach((requestId, index) => {
      requestPages[requestId] = <RequestDetailPage key={index} index={index} params={{requestId}} location={location} showBreadcrumbs={false} />
    });

    return (
      <div className="tabbed-page">
        <Row className="clearfix">
          <Col className="tab-col" sm={4} md={2}>
            <h3>Request Group</h3>
            <Row className="detail-header">
              <Col xs={10}>
                <h4>{group.id}</h4>
              </Col>
              <Col xs={2}>
                <ActionDropdown group={group} metadata={metadata} />
              </Col>
            </Row>

            <Nav bsStyle="pills" stacked={true} activeKey={this.state.showRequestId} onSelect={this.handleRequestSelect}>
              {group.requestIds.map((requestId, index) =>
                <NavItem className="request-group-navitem" key={index} eventKey={requestId}>
                  {requestId} {requestsNotFound.includes(requestId) && <Label bsStyle="danger">Deleted</Label>}
                </NavItem>
              )}
            </Nav>
          </Col>
          <Col sm={8} md={10}>
            {requestPages[this.state.showRequestId]}
          </Col>
        </Row>
      </div>
    );
  }
}

GroupDetail.propTypes = {
  group: PropTypes.object,
  location: PropTypes.object,
  requests: PropTypes.object,
  requestsNotFound: PropTypes.array
};

const mapStateToProps = (state, ownProps) => {
  const group = _.find(state.api.requestGroups.data, (filterGroup) => filterGroup.id === ownProps.params.groupId);
  return ({
    notFound: !state.api.requestGroups.isFetching && !group,
    pathname: ownProps.location.pathname,
    group,
    requestsNotFound: Object.entries(state.api.request).filter((entry) => group.requestIds.includes(entry[0]) && entry[1].statusCode === 404).map((entry) => entry[0])
  });
};

export default connect(mapStateToProps)(rootComponent(GroupDetail, refresh, false, false));
