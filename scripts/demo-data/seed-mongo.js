// Demo community posts (MongoDB). Run after seed-postgres.sql: the authors are its demo accounts.
//
//   mongosh "$STAGING_MONGO_URI" scripts/demo-data/seed-mongo.js
//
// Safe to run again. To remove the posts:
//
//   mongosh "$STAGING_MONGO_URI" --eval "var CLEANUP = true" scripts/demo-data/seed-mongo.js
//
// The app must have started once against this database, so collections and indexes exist.

const { createHash } = require('crypto');

// Same derivation as pg_temp.did in seed-postgres.sql.
function did(kind, n) {
    const h = createHash('md5').update('pet-rescue-demo:' + kind + ':' + n).digest('hex');
    return UUID(h.slice(0, 8) + '-' + h.slice(8, 12) + '-' + h.slice(12, 16) + '-' + h.slice(16, 20) + '-' + h.slice(20));
}

const POST_COUNT = 24;
const postIds = [];
for (let n = 1; n <= POST_COUNT; n++) postIds.push(did('post', n));

if (typeof CLEANUP !== 'undefined' && CLEANUP) {
    print('Removed posts: ' + db.posts.deleteMany({ _id: { $in: postIds } }).deletedCount);
    print('Removed comments: ' + db.comments.deleteMany({ postId: { $in: postIds } }).deletedCount);
    print('Removed likes: ' + db.post_likes.deleteMany({ '_id.postId': { $in: postIds } }).deletedCount);
} else {
    const texts = [
        'Hôm nay trạm vừa đón thêm một bé chó bị bỏ rơi, mọi người chung tay giúp bé nhé.',
        'Bé mèo này đã khỏe lại sau hai tuần điều trị, đang chờ gia đình mới.',
        'Tìm chủ cho bé cún đi lạc ở khu vực quận 1, bé rất ngoan và thân thiện.',
        'Cảm ơn các bạn đã hỗ trợ ca cứu hộ tuần trước, bé đã an toàn.',
        'Chia sẻ kinh nghiệm chăm sóc mèo con mới cai sữa cho người nuôi lần đầu.',
        'Cần tình nguyện viên hỗ trợ tiêm phòng cuối tuần này tại trạm cứu hộ.',
    ];
    const images = ['samples/animals/three-dogs', 'samples/animals/cat', 'samples/animals/kitten-playing', 'samples/animals/reindeer'];
    const tags = [['rescue'], ['adoption'], ['lost-pet'], ['rescue'], ['care'], ['volunteer']];
    const fullNames = ['Nguyễn An', 'Trần Bình', 'Lê Chi', 'Phạm Dũng', 'Hoàng Giang', 'Vũ Hà', 'Đặng Khoa', 'Bùi Linh'];

    let written = 0;
    for (let n = 1; n <= POST_COUNT; n++) {
        const authorN = 1 + (n % 30);
        const createdAt = new Date(Date.now() - n * 9 * 3600 * 1000);
        const res = db.posts.updateOne(
            { _id: did('post', n) },
            {
                $setOnInsert: {
                    author: {
                        userId: did('user', authorN),
                        username: 'demo.user' + authorN,
                        fullName: fullNames[authorN % 8] + ' ' + authorN,
                    },
                    content: texts[n % texts.length],
                    media: [{ mediaId: did('image', n % 4), publicId: images[n % 4] }],
                    tags: tags[n % tags.length],
                    stats: { likes: n * 3 % 40, comments: 0 },
                    previewComments: [],
                    createdAt: createdAt,
                    updatedAt: createdAt,
                    deleted: false,
                },
            },
            { upsert: true },
        );
        written += res.upsertedCount;
    }
    print('New posts: ' + written + ', total demo posts: ' + db.posts.countDocuments({ _id: { $in: postIds } }));
}
