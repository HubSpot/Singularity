import React, { Component, PropTypes } from 'react';
import Modal from 'react-bootstrap/lib/Modal';
import Button from 'react-bootstrap/lib/Button';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import JSONTree from 'react-json-tree';
import { JSONTreeTheme } from '../../thirdPartyConfigurations';
import CopyToClipboard from 'react-copy-to-clipboard';

export default class JSONButton extends Component {
  static propTypes = {
    children: PropTypes.oneOfType([
      PropTypes.arrayOf(PropTypes.node),
      PropTypes.node
    ]).isRequired,
    object: PropTypes.object.isRequired,
    showOverlay: PropTypes.bool,
    className: PropTypes.string,
    linkClassName: PropTypes.string
  };

  constructor() {
    super();
    this.state = {
      modalOpen: false
    };

    _.bindAll(this, 'showJSON', 'hideJSON');
  }

  showJSON() {
    this.setState({
      modalOpen: true
    });
  }

  hideJSON() {
    this.setState({
      modalOpen: false
    });
  }

  render() {
    const jsonTooltip = (
      <ToolTip id="view-json-tooltip">
        JSON
      </ToolTip>
    );
    const button = (
      <a className={this.props.linkClassName} onClick={this.showJSON} alt="Show JSON">{this.props.children}</a>
    );
    return (
      <span className={this.props.className}>
        {this.props.showOverlay ? (
          <OverlayTrigger placement="top" id="view-json-overlay" overlay={jsonTooltip}>
            {button}
          </OverlayTrigger>) : button
        }
        <Modal show={this.state.modalOpen} onHide={this.hideJSON} bsSize="large" enforceFocus={false}>
          <Modal.Body>
            <div className="constrained-modal json-modal">
              <JSONTree
                data={this.props.object}
                shouldExpandNode={() => true}
                theme={JSONTreeTheme}
              />
            </div>
          </Modal.Body>
          <Modal.Footer>
            <CopyToClipboard text={JSON.stringify(this.props.object, null, 2)}>
              <Button bsStyle="default" className="copy-btn">Copy</Button>
            </CopyToClipboard>
            <Button bsStyle="info" onClick={this.hideJSON}>Close</Button>
          </Modal.Footer>
        </Modal>
      </span>
    );
  }
}
