package com.ustadmobile.lib.contentscrapers.khanacademy;

import java.util.List;

class SubjectListResponse {

    public ComponentData componentProps;

    public class ComponentData {

        public Curation curation;

        public ItemResponse initialItem;

        public Card initialCards;

        public NavData tutorialNavData;

        public class Curation {

            public List<Tab> tabs;

            public class Tab {

                public List<ModuleResponse> modules;

            }
        }

        public class Card {

            public List<UserExercise> userExercises;

            public class UserExercise {

                public Model exerciseModel;

                public class Model {

                    public String id;

                    public String name;

                    public String dateModified;

                    public List<AssessmentItem> allAssessmentItems;

                    public class AssessmentItem {

                        public String id;

                        public boolean live;

                        public String sha;

                    }

                }


            }
        }

        public class NavData {

            public List<ContentModel> contentModels;

            public class ContentModel {

                public String id;

                public String nodeSlug;

                public String relativeUrl;

                public String slug;

                public String title;

                public String contentKind;

                public String kind;

            }
        }
    }
}
