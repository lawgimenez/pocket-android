## Prepare the release
* [ ] If modified sync engine schema, double check the usage file only contains the final new definitions.
* [ ] Prepare a pre-release in GitHub releases to generate a changelog.
  * [ ] Use this link template, replacing `X.Y.Z` with release version (in 2 places): `https://github.com/Pocket/pocket-android/releases/new?tag=X.Y.Z&target=release-X.Y.Z&prerelease=1`. (This will set some defaults, so you don't have to edit by hand.)
  * [ ] Use the "Generate release notes" button to use an auto-generated changelog.
  * [ ] Add a summary to the top of the changelog.
  * **Don't publish just yet!**
* [ ] Wait for CI to push the "release prep" commit.
  * [ ] Check merged manifest changes:
    * [ ] Check that no additional app permissions have been unintentionally added. This can occur when adding new dependencies or upgrading existing ones. If permissions have been added, check that these are actually required for the production version of the app before submitting and if so notify customer support of the change and reason for it.
    * [ ] Skim other changes to see if they are what you'd expect and there isn't anything eyebrow-raising.
  * [ ] Publish GitHub release you prepared above.
* [ ] Post to #pocket-releases using the workflow pinned to the top of the channel.

## Release on Google Play
* [ ] Promote beta ([open testing](https://play.google.com/console/u/0/developers/5995605107085635372/app/4974611608118969152/tracks/open-testing)) to production.
  * [ ] Always update the release notes, because we don't want to keep the beta copy. If there is no prepared, agreed upon copy to call out new features, copy standard "bug fixes and improvements" notes from a previous **production** release.
  * [ ] Set rollout percentage (usually to 100%, in case of bigger/riskier releases to 10%, discuss with the team if you think it should be something else).
* [ ] Release a new beta by promoting the [internal test](https://play.google.com/console/u/0/developers/5995605107085635372/app/4974611608118969152/tracks/internal-testing) build to beta (open testing).
  * [ ] Copy previous **beta** release notes.
  * [ ] Check "Errors, warnings and messages". Google loves to put stuff in there that isn't really a problem, so there are "warnings" there we consistently ignore with each release. But, please, always check it to see if there isn't anything important this time.
  * [ ] Check changes between this and previous APK. (New App Bundles > Details (→ to the right of the new APK) > Changes tab)
  * [ ] Set rollout percentage to 100%.
* [ ] Go to [Publishing overview](https://play.google.com/console/u/0/developers/5995605107085635372/app/4974611608118969152/publishing) to send the changes for review.
  * [ ] Check the current Managed publishing setting. Keep if **off** to release as soon as it's through the review (e.g. when sending to review on the day of the release). Turn it **on** if preparing the release a day ahead and you want to manually release next morning.

## After the release is live
* Production
  * [ ] Post an update to #pocket-releases about the current rollout percentage.
  * [ ] Add the release to [Confluence](https://mozilla-hub.atlassian.net/wiki/spaces/PE/pages/665878545/Changelog+Releases+Pocket).
  * [ ] Create a PR of `beta` into `prod` using this [template](https://github.com/Pocket/pocket-android/compare/prod...beta?quick_pull=1&title=X.Y.Z.0+Production&body=Updating+`prod`+to+match+the+newly+promoted+build.&labels=ignore-for-release). Merge it (**do not squash!**).
* Beta
  * [ ] Edit the GitHub release, uncheck "Set as a pre-release", check "Set as the latest release".
  * [ ] Merge this PR into `beta` (**do not squash!**).
  * [ ] Create a PR of `beta` into `main`. (`beta` is protected, so we can't literally merge it, because it would delete it. Instead create a new branch from `beta`, merge `main` to resolve any conflicts and open a PR to merge this branch to `main`.)
    * [ ] Add `ignore-for-release` label.
    * [ ] Merge it (**do not squash!**).
