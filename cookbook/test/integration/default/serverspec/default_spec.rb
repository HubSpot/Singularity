# Encoding: utf-8
require 'spec_helper'

describe 'Filesystem' do
  describe file('/usr/local/singularity') do
    it { should be_directory }
    it { should be_owned_by 'singularity' }
  end
end
