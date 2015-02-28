source 'https://rubygems.org'

group :test, :development do
  gem 'rake'
end

group :test do
  gem 'berkshelf',  '~> 3.2'
end

group :test, :integration do
  gem 'test-kitchen',
      github: 'test-kitchen/test-kitchen',
      tag: '4dc905a74bd86257e9f01bd91b06c0c18515763c'
  gem 'serverspec', '~> 2.7'
end

group :test, :vagrant do
  gem 'kitchen-vagrant', '~> 0.15'
end
