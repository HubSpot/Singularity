import React from 'react';
import { connect } from 'react-redux';
import Utils from '../../utils';

const Footer = ({filename, bytesTotal, bytesLoaded}) => (<div>{filename} ({Utils.humanizeFileSize(bytesTotal)} total, {Utils.humanizeFileSize(bytesLoaded)} loaded)</div>);

export default connect((state, ownProps) => {
  return {
    filename: Utils.fileName(ownProps.tailerId),
    bytesTotal: Utils.maybe(state.tailer.files, [ownProps.tailerId, 'fileSize'], 0),
    bytesLoaded: Utils.maybe(state.tailer.files, [ownProps.tailerId, 'chunks'], []).map(({byteLength}) => byteLength).reduce((a, b) => a + b, 0)
  };
})(Footer);