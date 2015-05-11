# Encoding: utf-8
require 'spec_helper'

describe 'Filesystem' do
  describe file('/usr/local/singularity') do
    it { should be_directory }
    it { should be_owned_by 'singularity' }
  end
end

context 'Configuration' do
  describe file('/etc/singularity/singularity.yaml') do
    it { is_expected.to be_file }
    describe '#content' do
      subject { super().content }
      it do
        is_expected.to(
          match(
            %r{^loadBalancerUri: http:\/\/localhost:8088/baragon/v2/request}
          )
        )
      end
    end
  end
end
