import React, {PropTypes, Component} from 'react';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';

import { Row, Col, Nav, NavItem } from 'react-bootstrap';
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
    const {group, location} = this.props;
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
          <Col className="tab-col" sm={2}>
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
              {group.requestIds.map((requestId, index) => <NavItem key={index} eventKey={requestId}>{requestId}</NavItem>)}
            </Nav>
          </Col>
          <Col sm={10}>
            {requestPages[this.state.showRequestId]}
          </Col>
        </Row>
      </div>
    );
  }

};

GroupDetail.propTypes = {
  group: PropTypes.object,
  location: PropTypes.object,
  requests: PropTypes.object
};

const mapStateToProps = (state, ownProps) => {
  const group = _.find(state.api.requestGroups.data, (filterGroup) => filterGroup.id === ownProps.params.groupId);
  return ({
    notFound: !state.api.requestGroups.isFetching && !group,
    pathname: ownProps.location.pathname,
    group
  });
};

export default connect(mapStateToProps)(rootComponent(GroupDetail, refresh, false, false));