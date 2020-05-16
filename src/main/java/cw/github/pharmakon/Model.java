package cw.github.pharmakon;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;



public class Model {

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Getter
	@Setter
	@NoArgsConstructor
	public static class Owner implements Serializable {

		@JsonProperty("login")
		private String login;
		@JsonProperty("id")
		private Integer id;
		@JsonProperty("node_id")
		private String nodeId;
		@JsonProperty("avatar_url")
		private String avatarUrl;
		@JsonProperty("gravatar_id")
		private String gravatarId;
		@JsonProperty("url")
		private String url;
		@JsonProperty("html_url")
		private String htmlUrl;
		@JsonProperty("followers_url")
		private String followersUrl;
		@JsonProperty("following_url")
		private String followingUrl;
		@JsonProperty("gists_url")
		private String gistsUrl;
		@JsonProperty("starred_url")
		private String starredUrl;
		@JsonProperty("subscriptions_url")
		private String subscriptionsUrl;
		@JsonProperty("organizations_url")
		private String organizationsUrl;
		@JsonProperty("repos_url")
		private String reposUrl;
		@JsonProperty("events_url")
		private String eventsUrl;
		@JsonProperty("received_events_url")
		private String receivedEventsUrl;
		@JsonProperty("type")
		private String type;
		@JsonProperty("site_admin")
		private Boolean siteAdmin;
		@JsonIgnore
		private Map<String, Object> additionalProperties = new HashMap<String, Object>();
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Getter
	@Setter
	@NoArgsConstructor
	public static class Permissions implements Serializable {

		@JsonProperty("admin")
		private Boolean admin;
		@JsonProperty("push")
		private Boolean push;
		@JsonProperty("pull")
		private Boolean pull;
		@JsonIgnore
		private Map<String, Object> additionalProperties = new HashMap<String, Object>();

	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Getter
	@Setter
	@NoArgsConstructor
	public static class License implements Serializable {

		@JsonProperty("key")
		private String key;
		@JsonProperty("name")
		private String name;
		@JsonProperty("spdx_id")
		private String spdxId;
		@JsonProperty("url")
		private String url;
		@JsonProperty("node_id")
		private String nodeId;
		@JsonIgnore
		private Map<String, Object> additionalProperties = new HashMap<String, Object>();

	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Repositories implements Serializable {

		@JsonProperty("total_count")
		private Integer totalCount;
		@JsonProperty("incomplete_results")
		private Boolean incompleteResults;
		@JsonProperty("items")
		private List<Repository> items = null;
		@JsonIgnore
		private Map<String, Object> additionalProperties = new HashMap<String, Object>();

	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Getter
	@Setter
	@NoArgsConstructor
	public static class Repository implements Serializable {

		@JsonProperty("id")
		private Integer id;
		@JsonProperty("node_id")
		private String nodeId;
		@JsonProperty("name")
		private String name;
		@JsonProperty("full_name")
		private String fullName;
		@JsonProperty("private")
		private Boolean _private;
		@JsonProperty("owner")
		private Owner owner;
		@JsonProperty("html_url")
		private String htmlUrl;
		@JsonProperty("description")
		private String description;
		@JsonProperty("fork")
		private Boolean fork;
		@JsonProperty("url")
		private String url;
		@JsonProperty("forks_url")
		private String forksUrl;
		@JsonProperty("keys_url")
		private String keysUrl;
		@JsonProperty("collaborators_url")
		private String collaboratorsUrl;
		@JsonProperty("teams_url")
		private String teamsUrl;
		@JsonProperty("hooks_url")
		private String hooksUrl;
		@JsonProperty("issue_events_url")
		private String issueEventsUrl;
		@JsonProperty("events_url")
		private String eventsUrl;
		@JsonProperty("assignees_url")
		private String assigneesUrl;
		@JsonProperty("branches_url")
		private String branchesUrl;
		@JsonProperty("tags_url")
		private String tagsUrl;
		@JsonProperty("blobs_url")
		private String blobsUrl;
		@JsonProperty("git_tags_url")
		private String gitTagsUrl;
		@JsonProperty("git_refs_url")
		private String gitRefsUrl;
		@JsonProperty("trees_url")
		private String treesUrl;
		@JsonProperty("statuses_url")
		private String statusesUrl;
		@JsonProperty("languages_url")
		private String languagesUrl;
		@JsonProperty("stargazers_url")
		private String stargazersUrl;
		@JsonProperty("contributors_url")
		private String contributorsUrl;
		@JsonProperty("subscribers_url")
		private String subscribersUrl;
		@JsonProperty("subscription_url")
		private String subscriptionUrl;
		@JsonProperty("commits_url")
		private String commitsUrl;
		@JsonProperty("git_commits_url")
		private String gitCommitsUrl;
		@JsonProperty("comments_url")
		private String commentsUrl;
		@JsonProperty("issue_comment_url")
		private String issueCommentUrl;
		@JsonProperty("contents_url")
		private String contentsUrl;
		@JsonProperty("compare_url")
		private String compareUrl;
		@JsonProperty("merges_url")
		private String mergesUrl;
		@JsonProperty("archive_url")
		private String archiveUrl;
		@JsonProperty("downloads_url")
		private String downloadsUrl;
		@JsonProperty("issues_url")
		private String issuesUrl;
		@JsonProperty("pulls_url")
		private String pullsUrl;
		@JsonProperty("milestones_url")
		private String milestonesUrl;
		@JsonProperty("notifications_url")
		private String notificationsUrl;
		@JsonProperty("labels_url")
		private String labelsUrl;
		@JsonProperty("releases_url")
		private String releasesUrl;
		@JsonProperty("deployments_url")
		private String deploymentsUrl;
		@JsonProperty("created_at")
		private String createdAt;
		@JsonProperty("updated_at")
		private String updatedAt;
		@JsonProperty("pushed_at")
		private String pushedAt;
		@JsonProperty("git_url")
		private String gitUrl;
		@JsonProperty("ssh_url")
		private String sshUrl;
		@JsonProperty("clone_url")
		private String cloneUrl;
		@JsonProperty("svn_url")
		private String svnUrl;
		@JsonProperty("homepage")
		private String homepage;
		@JsonProperty("size")
		private Integer size;
		@JsonProperty("stargazers_count")
		private Integer stargazersCount;
		@JsonProperty("watchers_count")
		private Integer watchersCount;
		@JsonProperty("language")
		private String language;
		@JsonProperty("has_issues")
		private Boolean hasIssues;
		@JsonProperty("has_projects")
		private Boolean hasProjects;
		@JsonProperty("has_downloads")
		private Boolean hasDownloads;
		@JsonProperty("has_wiki")
		private Boolean hasWiki;
		@JsonProperty("has_pages")
		private Boolean hasPages;
		@JsonProperty("forks_count")
		private Integer forksCount;
		@JsonProperty("mirror_url")
		private Object mirrorUrl;
		@JsonProperty("archived")
		private Boolean archived;
		@JsonProperty("disabled")
		private Boolean disabled;
		@JsonProperty("open_issues_count")
		private Integer openIssuesCount;
		@JsonProperty("license")
		private License license;
		@JsonProperty("forks")
		private Integer forks;
		@JsonProperty("open_issues")
		private Integer openIssues;
		@JsonProperty("watchers")
		private Integer watchers;
		@JsonProperty("default_branch")
		private String defaultBranch;
		@JsonProperty("permissions")
		private Permissions permissions;
		@JsonProperty("score")
		private Double score;
		@JsonIgnore
		private Map<String, Object> additionalProperties = new HashMap<String, Object>();
	}

}
